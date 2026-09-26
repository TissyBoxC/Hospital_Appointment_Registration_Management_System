package io.github.tissyboxc.harmsys.appointment.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 管理员预约查询、号源变更和退款的数据访问层。 */
@Repository
public class AdminAppointmentRepository {
  private final JdbcTemplate jdbc;

  public AdminAppointmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> listAll() {
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name"
            + " FROM appointment a JOIN patient p ON p.id=a.patient_id JOIN doctor d ON"
            + " d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id"
            + " ORDER BY a.appointment_date DESC,a.id DESC");
  }

  public List<Map<String, Object>> page(Integer status, int size, int offset) {
    String sql =
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name"
            + " FROM appointment a JOIN patient p ON p.id=a.patient_id JOIN doctor d ON"
            + " d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id"
            + (status == null ? "" : " WHERE a.status=?")
            + " ORDER BY a.appointment_date DESC,a.id DESC LIMIT ? OFFSET ?";
    return status == null
        ? jdbc.queryForList(sql, size, offset)
        : jdbc.queryForList(sql, status, size, offset);
  }

  public long total(Integer status) {
    Long value =
        status == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM appointment a", Long.class)
            : jdbc.queryForObject(
                "SELECT COUNT(*) FROM appointment a WHERE a.status=?", Long.class, status);
    return value == null ? 0 : value;
  }

  public Map<String, Object> detail(long id) {
    return one(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone,d.real_name doctor_name,"
            + "dp.name department_name FROM appointment a JOIN patient p ON p.id=a.patient_id"
            + " JOIN doctor d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id"
            + " WHERE a.id=?",
        id);
  }

  public Map<String, Object> lockAppointment(long id) {
    return one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockSlot(long slotId, long scheduleId) {
    return one(
        "SELECT * FROM schedule_slot WHERE id=? AND schedule_id=? FOR UPDATE", slotId, scheduleId);
  }

  public long countActiveForPatientAndSchedule(long patientId, long scheduleId, long exceptId) {
    Long value =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointment WHERE patient_id=? AND schedule_id=? AND id<>?"
                + " AND status IN (1,2,3,4)",
            Long.class,
            patientId,
            scheduleId,
            exceptId);
    return value == null ? 0 : value;
  }

  public int releaseSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public int reserveSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=1 WHERE id=? AND status=0", slotId);
  }

  public int decrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
  }

  public int incrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=? AND"
            + " booked_count<total_count",
        scheduleId);
  }

  public int incrementBookedCountUnchecked(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=?", scheduleId);
  }

  public int updateAppointment(long id, Integer queueNo, int status, String remark) {
    return jdbc.update(
        "UPDATE appointment SET queue_no=?,status=?,remark=? WHERE id=?",
        queueNo,
        status,
        remark,
        id);
  }

  public int migrateAppointment(
      long id,
      Object doctorId,
      Object departmentId,
      long scheduleId,
      Long slotId,
      Object appointmentDate,
      Object period,
      Object fee) {
    return jdbc.update(
        "UPDATE appointment SET"
            + " doctor_id=?,department_id=?,schedule_id=?,slot_id=?,appointment_date=?,period=?,fee=?"
            + " WHERE id=?",
        doctorId,
        departmentId,
        scheduleId,
        slotId,
        appointmentDate,
        period,
        fee,
        id);
  }

  public int refundSuccessfulPayment(long appointmentId) {
    return jdbc.update(
        "UPDATE payment_record SET"
            + " status=5,refunded_at=COALESCE(refunded_at,CURRENT_TIMESTAMP),refund_reason=COALESCE(refund_reason,'管理员修改预约状态')"
            + " WHERE appointment_id=? AND status=2",
        appointmentId);
  }

  public int refundSuccessfulPayment(long appointmentId, String reason) {
    return jdbc.update(
        "UPDATE payment_record SET"
            + " status=5,refunded_at=CURRENT_TIMESTAMP,refund_reason=COALESCE(?,refund_reason)"
            + " WHERE appointment_id=? AND status=2",
        reason,
        appointmentId);
  }

  public int cancel(long id, String reason) {
    return jdbc.update(
        "UPDATE appointment SET status=7,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE id=?",
        reason,
        id);
  }

  public int expire(long id) {
    return jdbc.update(
        "UPDATE appointment SET status=8,cancel_reason='管理员标记过期' WHERE id=? AND status IN (1,2)",
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
