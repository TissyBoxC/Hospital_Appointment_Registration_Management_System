package io.github.tissyboxc.harmsys.doctor.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 排班生命周期、停诊与时间段维护的数据访问层。 */
@Repository
public class ScheduleLifecycleRepository {
  private final JdbcTemplate jdbc;

  public ScheduleLifecycleRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> lockSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockDoctorSchedule(long id, long doctorId) {
    return one(
        "SELECT * FROM doctor_schedule WHERE id=? AND doctor_id=? FOR UPDATE", id, doctorId);
  }

  public Map<String, Object> findSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=?", id);
  }

  public Map<String, Object> findSlot(long slotId) {
    return one("SELECT * FROM schedule_slot WHERE id=?", slotId);
  }

  public int updateScheduleStatus(long id, int status) {
    return jdbc.update("UPDATE doctor_schedule SET status=? WHERE id=?", status, id);
  }

  public int updateSlotStatus(long slotId, int status) {
    return jdbc.update("UPDATE schedule_slot SET status=? WHERE id=?", status, slotId);
  }

  public int deleteSlot(long slotId) {
    return jdbc.update("DELETE FROM schedule_slot WHERE id=?", slotId);
  }

  public long countBlockingAppointments(long slotId) {
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointment WHERE slot_id=? AND status IN (1,2,3,4)",
            Long.class,
            slotId);
    return count == null ? 0 : count;
  }

  public int stopSchedule(long id, String reason) {
    return jdbc.update("UPDATE doctor_schedule SET status=2,remark=? WHERE id=?", reason, id);
  }

  public List<Map<String, Object>> activeAppointments(long scheduleId) {
    return jdbc.queryForList(
        "SELECT id,slot_id FROM appointment WHERE schedule_id=? AND status IN (1,2,3,4)",
        scheduleId);
  }

  public int cancelAppointment(long id, String reason) {
    return jdbc.update(
        "UPDATE appointment SET status=7,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE"
            + " id=?",
        reason,
        id);
  }

  public int releaseSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public int refundSuccessfulPayment(long appointmentId) {
    return jdbc.update(
        "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE appointment_id=?"
            + " AND status=2",
        appointmentId);
  }

  public int resetBookedCount(long scheduleId) {
    return jdbc.update("UPDATE doctor_schedule SET booked_count=0 WHERE id=?", scheduleId);
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
