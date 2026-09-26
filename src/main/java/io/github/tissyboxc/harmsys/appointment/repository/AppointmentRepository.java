package io.github.tissyboxc.harmsys.appointment.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 患者预约事务的数据访问层。 */
@Repository
public class AppointmentRepository {
  private final JdbcTemplate jdbc;

  public AppointmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> findIdempotentAppointment(String requestNo, long patientId) {
    return one(
        "SELECT appointment_id FROM appointment_idempotency WHERE request_no=? AND patient_id=?",
        requestNo,
        patientId);
  }

  public Map<String, Object> lockSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockSlot(long slotId, long scheduleId) {
    return one("SELECT * FROM schedule_slot WHERE id=? AND schedule_id=? FOR UPDATE", slotId, scheduleId);
  }

  public boolean doctorAvailable(long doctorId) {
    return count("SELECT COUNT(*) FROM doctor WHERE id=? AND status=1 AND deleted=0", doctorId) > 0;
  }

  public long countPatientActiveAppointments(long patientId, long scheduleId) {
    return count(
        "SELECT COUNT(*) FROM appointment WHERE patient_id=? AND schedule_id=? AND status IN"
            + " (1,2,3,4)",
        patientId,
        scheduleId);
  }

  public int nextQueueNo(long scheduleId) {
    Integer value =
        jdbc.queryForObject(
            "SELECT COALESCE(MAX(queue_no),0)+1 FROM appointment WHERE schedule_id=? FOR UPDATE",
            Integer.class,
            scheduleId);
    return value == null ? 1 : value;
  }

  public long insertAppointment(
      String appointmentNo,
      long patientId,
      long doctorId,
      Map<String, Object> schedule,
      long scheduleId,
      Long slotId,
      int queueNo,
      BigDecimal fee,
      String remark) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO"
                          + " appointment(appointment_no,patient_id,doctor_id,department_id,schedule_id,slot_id,appointment_date,period,queue_no,fee,status,remark)"
                          + " VALUES(?,?,?,?,?,?,?,?,?,?,2,?)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setString(1, appointmentNo);
              statement.setLong(2, patientId);
              statement.setLong(3, doctorId);
              statement.setObject(4, schedule.get("department_id"));
              statement.setLong(5, scheduleId);
              statement.setObject(6, slotId);
              statement.setObject(7, schedule.get("schedule_date"));
              statement.setObject(8, schedule.get("period"));
              statement.setInt(9, queueNo);
              statement.setBigDecimal(10, fee);
              statement.setString(11, remark);
              return statement;
            },
            holder);
    if (rows != 1 || holder.getKey() == null) throw new IllegalStateException("创建预约失败");
    return holder.getKey().longValue();
  }

  public int reserveSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=1 WHERE id=? AND status=0", slotId);
  }

  public int incrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=? AND booked_count <"
            + " total_count",
        scheduleId);
  }

  public void insertIdempotency(String requestNo, long patientId, long appointmentId) {
    jdbc.update(
        "INSERT INTO appointment_idempotency(request_no,patient_id,appointment_id) VALUES(?,?,?)",
        requestNo,
        patientId,
        appointmentId);
  }

  public void insertSuccessfulPayment(
      String paymentNo, long appointmentId, long patientId, BigDecimal fee) {
    jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?, ?,1,2,?,CURRENT_TIMESTAMP)",
        paymentNo,
        appointmentId,
        patientId,
        fee,
        "MOCK-" + paymentNo);
  }

  public void insertOperationLog(
      long userId, String type, String target, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        id,
        description,
        ipAddress);
  }

  public void insertNotification(
      long userId, String title, String content, String notificationType) {
    jdbc.update(
        "INSERT INTO system_notification(user_id,title,content,notification_type) VALUES(?,?,?,?)",
        userId,
        title,
        content,
        notificationType);
  }

  public String recipientPhoneForUser(long userId) {
    return jdbc.query(
        "SELECT phone FROM patient WHERE user_id=? UNION ALL SELECT NULL FROM doctor WHERE"
            + " user_id=? LIMIT 1",
        rs -> rs.next() ? rs.getString(1) : null,
        userId,
        userId);
  }

  public void insertOutbox(
      long userId, String channel, String recipient, String subject, String content) {
    jdbc.update(
        "INSERT INTO notification_outbox(user_id,channel,recipient,subject,content,status)"
            + " VALUES(?,?,?,?,?,0)",
        userId,
        channel,
        recipient,
        subject,
        content);
  }

  public Long doctorUserId(long doctorId) {
    return jdbc.query(
        "SELECT user_id FROM doctor WHERE id=?",
        rs -> rs.next() ? rs.getLong(1) : null,
        doctorId);
  }

  public Map<String, Object> detail(long appointmentId) {
    return one(
        "SELECT a.*,d.real_name doctor_name,dp.name department_name,p.real_name patient_name FROM"
            + " appointment a JOIN doctor d ON d.id=a.doctor_id JOIN department dp ON"
            + " dp.id=a.department_id JOIN patient p ON p.id=a.patient_id WHERE a.id=?",
        appointmentId);
  }

  public Map<String, Object> lockAppointment(long id) {
    return one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockPatientAppointment(long id, long patientId) {
    return one("SELECT * FROM appointment WHERE id=? AND patient_id=? FOR UPDATE", id, patientId);
  }

  public java.sql.Time slotStartTime(long slotId) {
    return jdbc.query(
        "SELECT start_time FROM schedule_slot WHERE id=?",
        rs -> rs.next() ? rs.getTime(1) : null,
        slotId);
  }

  public int cancelCutoffMinutes() {
    try {
      Integer value =
          jdbc.queryForObject(
              "SELECT CAST(config_value AS UNSIGNED) FROM system_config WHERE"
                  + " config_key='appointment.cancel.cutoff.minutes'",
              Integer.class);
      return value == null ? 30 : value;
    } catch (Exception ignored) {
      return 30;
    }
  }

  public int cancelAppointment(long appointmentId, String reason) {
    return jdbc.update(
        "UPDATE appointment SET status=6,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE id=?",
        reason,
        appointmentId);
  }

  public int releaseSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public int decrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
  }

  public int refundSuccessfulPayment(long appointmentId) {
    return jdbc.update(
        "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE appointment_id=?"
            + " AND status=2",
        appointmentId);
  }

  public int checkIn(long appointmentId) {
    return jdbc.update("UPDATE appointment SET status=3 WHERE id=?", appointmentId);
  }

  public boolean visitExists(long appointmentId) {
    return count("SELECT COUNT(*) FROM medical_visit WHERE appointment_id=?", appointmentId) > 0;
  }

  public void insertVisit(
      long appointmentId, Object patientId, Object doctorId, String visitNo) {
    jdbc.update(
        "INSERT INTO"
            + " medical_visit(appointment_id,patient_id,doctor_id,visit_no,status,check_in_at)"
            + " VALUES(?,?,?,?,1,CURRENT_TIMESTAMP)",
        appointmentId,
        patientId,
        doctorId,
        visitNo);
  }

  public List<Map<String, Object>> patientAppointments(long patientId) {
    return jdbc.queryForList(
        "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN doctor"
            + " d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
            + " a.patient_id=? ORDER BY a.appointment_date DESC,a.id DESC",
        patientId);
  }

  public List<Map<String, Object>> doctorAppointments(long doctorId) {
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone FROM appointment a JOIN patient"
            + " p ON p.id=a.patient_id WHERE a.doctor_id=? ORDER BY"
            + " a.appointment_date,a.queue_no,a.id",
        doctorId);
  }

  public List<Map<String, Object>> registrationAppointments() {
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE a.status IN"
            + " (1,2,3) ORDER BY a.appointment_date,a.queue_no,a.id");
  }

  public List<Map<String, Object>> queue() {
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE a.status=3"
            + " ORDER BY a.queue_no,a.id");
  }

  public Map<String, Object> patientPage(
      long patientId, Integer status, int size, int offset) {
    String sql =
        "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN doctor"
            + " d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
            + " a.patient_id=?"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date DESC,a.id DESC LIMIT ? OFFSET ?";
    List<Map<String, Object>> items =
        status == null
            ? jdbc.queryForList(sql, patientId, size, offset)
            : jdbc.queryForList(sql, patientId, status, size, offset);
    long total =
        status == null
            ? count("SELECT COUNT(*) FROM appointment WHERE patient_id=?", patientId)
            : count(
                "SELECT COUNT(*) FROM appointment WHERE patient_id=? AND status=?",
                patientId,
                status);
    return page(items, total, size, offset);
  }

  public Map<String, Object> doctorPage(long doctorId, Integer status, int size, int offset) {
    String sql =
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone FROM appointment a JOIN patient"
            + " p ON p.id=a.patient_id WHERE a.doctor_id=?"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?";
    List<Map<String, Object>> items =
        status == null
            ? jdbc.queryForList(sql, doctorId, size, offset)
            : jdbc.queryForList(sql, doctorId, status, size, offset);
    long total =
        status == null
            ? count("SELECT COUNT(*) FROM appointment WHERE doctor_id=?", doctorId)
            : count(
                "SELECT COUNT(*) FROM appointment WHERE doctor_id=? AND status=?",
                doctorId,
                status);
    return page(items, total, size, offset);
  }

  public Map<String, Object> registrationPage(Integer status, int size, int offset) {
    String sql =
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE 1=1"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?";
    List<Map<String, Object>> items =
        status == null
            ? jdbc.queryForList(sql, size, offset)
            : jdbc.queryForList(sql, status, size, offset);
    long total =
        status == null
            ? count("SELECT COUNT(*) FROM appointment WHERE 1=1")
            : count("SELECT COUNT(*) FROM appointment WHERE 1=1 AND status=?", status);
    return page(items, total, size, offset);
  }

  public Long patientUserId(long patientId) {
    return jdbc.query(
        "SELECT user_id FROM patient WHERE id=? AND deleted=0",
        rs -> rs.next() ? rs.getLong(1) : null,
        patientId);
  }

  private Map<String, Object> page(
      List<Map<String, Object>> items, long total, int size, int offset) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", offset / size + 1);
    result.put("page_size", size);
    result.put("total", total);
    result.put("items", items);
    return result;
  }

  private long count(String sql, Object... args) {
    Long value = jdbc.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
