package io.github.tissyboxc.harmsys.registration.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 挂号员代挂号、退款和队列操作的数据访问层。 */
@Repository
public class RegistrationOperationsRepository {
  private final JdbcTemplate jdbc;

  public RegistrationOperationsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> patients(String keyword) {
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    return jdbc.queryForList(
        "SELECT"
            + " id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone"
            + " FROM patient WHERE deleted=0 AND (real_name LIKE ? OR phone LIKE ? OR id_card LIKE"
            + " ?) ORDER BY id DESC LIMIT 100",
        like,
        like,
        like);
  }

  public Map<String, Object> patient(long id) {
    return one(
        "SELECT"
            + " id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone"
            + " FROM patient WHERE id=? AND deleted=0",
        id);
  }

  public boolean patientExists(long id) {
    Long value =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM patient WHERE id=? AND deleted=0", Long.class, id);
    return value != null && value > 0;
  }

  public Map<String, Object> lockSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
  }

  public int reserveSlot(long slotId, long scheduleId) {
    return jdbc.update(
        "UPDATE schedule_slot SET status=1 WHERE id=? AND schedule_id=? AND status=0",
        slotId,
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
      Map<String, Object> schedule,
      long scheduleId,
      Long slotId,
      int queueNo,
      String remark) {
    jdbc.update(
        "INSERT INTO"
            + " appointment(appointment_no,patient_id,doctor_id,department_id,schedule_id,slot_id,appointment_date,period,queue_no,fee,status,remark)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,2,?)",
        appointmentNo,
        patientId,
        schedule.get("doctor_id"),
        schedule.get("department_id"),
        scheduleId,
        slotId,
        schedule.get("schedule_date"),
        schedule.get("period"),
        queueNo,
        schedule.get("fee"),
        remark);
    Long id =
        jdbc.queryForObject(
            "SELECT id FROM appointment WHERE appointment_no=?", Long.class, appointmentNo);
    if (id == null) throw new IllegalStateException("创建预约失败");
    return id;
  }

  public int incrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=?", scheduleId);
  }

  public void insertSuccessfulPayment(
      String paymentNo, long appointmentId, long patientId, BigDecimal amount) {
    jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?,?,1,2,?,CURRENT_TIMESTAMP)",
        paymentNo,
        appointmentId,
        patientId,
        amount,
        "MOCK-" + paymentNo);
  }

  public Map<String, Object> lockAppointment(long id) {
    return one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
  }

  public int refundPayment(long appointmentId) {
    return jdbc.update(
        "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE"
            + " appointment_id=? AND status=2",
        appointmentId);
  }

  public int markRefundedAppointment(long id) {
    return jdbc.update(
        "UPDATE appointment SET status=9,cancel_reason='挂号员退款' WHERE id=? AND status IN"
            + " (1,2,3,4)",
        id);
  }

  public int releaseSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public int decrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
  }

  public int callNext(long appointmentId) {
    return jdbc.update(
        "UPDATE appointment SET status=4 WHERE id=? AND status=3", appointmentId);
  }

  public int startVisitForAppointment(long appointmentId) {
    return jdbc.update(
        "UPDATE medical_visit SET"
            + " status=2,visit_start_at=COALESCE(visit_start_at,CURRENT_TIMESTAMP) WHERE"
            + " appointment_id=?",
        appointmentId);
  }

  public int markNoShow(long appointmentId) {
    return jdbc.update(
        "UPDATE appointment SET status=8,cancel_reason='患者过号' WHERE id=? AND status=3",
        appointmentId);
  }

  public int requeue(long appointmentId) {
    return jdbc.update(
        "UPDATE appointment SET status=3 WHERE id=? AND status=8", appointmentId);
  }

  public Map<String, Object> appointment(long id) {
    return one("SELECT * FROM appointment WHERE id=?", id);
  }

  public Map<String, Object> appointmentDetail(long id) {
    return one(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id"
            + " JOIN department dp ON dp.id=a.department_id WHERE a.id=?",
        id);
  }

  public void insertOperationLog(
      long userId, String type, long appointmentId, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        "appointment",
        appointmentId,
        description,
        ipAddress);
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> result = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      result.put(metadata.getColumnLabel(i), rs.getObject(i));
    return result;
  }
}
