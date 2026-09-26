package io.github.tissyboxc.harmsys.system.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 后台业务状态维护的数据访问层。 */
@Repository
public class BusinessMaintenanceRepository {
  private final JdbcTemplate jdbc;

  public BusinessMaintenanceRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void activateDueSchedules() {
    jdbc.update(
        "UPDATE doctor_schedule SET status=1 WHERE status=0 AND schedule_date=CURRENT_DATE AND"
            + " start_time>CURTIME() AND EXISTS (SELECT 1 FROM doctor d WHERE"
            + " d.id=doctor_schedule.doctor_id AND d.status=1 AND d.deleted=0)");
  }

  public void closeExpiredSchedules() {
    jdbc.update(
        "UPDATE doctor_schedule SET status=3 WHERE status=1 AND (schedule_date<CURRENT_DATE OR"
            + " (schedule_date=CURRENT_DATE AND end_time<=CURTIME()))");
  }

  public List<Map<String, Object>> findExpiredAppointments() {
    return jdbc.queryForList(
        "SELECT id,schedule_id,slot_id FROM appointment WHERE status IN (1,2) AND"
            + " ((appointment_date<CURRENT_DATE) OR (appointment_date=CURRENT_DATE AND"
            + " created_at < DATE_SUB(NOW(), INTERVAL 1 DAY)))");
  }

  public boolean expireAppointment(long appointmentId) {
    return jdbc.update(
            "UPDATE appointment SET status=8,cancel_reason='系统自动过期' WHERE id=? AND status IN"
                + " (1,2)",
            appointmentId)
        == 1;
  }

  public void releaseSlot(long slotId) {
    jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public void decrementBookedCount(long scheduleId) {
    jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
  }

  public void markNoShows() {
    jdbc.update(
        "UPDATE appointment a LEFT JOIN medical_visit v ON v.appointment_id=a.id SET"
            + " a.status=8,a.cancel_reason='系统自动标记过号' WHERE a.status=3 AND"
            + " a.appointment_date<CURRENT_DATE AND v.id IS NULL");
  }

  public void reconcileBookedCounts() {
    jdbc.update(
        "UPDATE doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt FROM appointment"
            + " WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON x.schedule_id=ds.id SET"
            + " ds.booked_count=COALESCE(x.cnt,0) WHERE ds.booked_count<>COALESCE(x.cnt,0)");
  }

  public void repairOrphanSlots() {
    jdbc.update(
        "UPDATE schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND a.status IN (1,2,3,4)"
            + " SET s.status=0 WHERE s.status=1 AND a.id IS NULL");
  }

  public void reconcilePayments() {
    jdbc.update(
        "UPDATE payment_record p JOIN appointment a ON a.id=p.appointment_id SET"
            + " p.status=5,p.refunded_at=COALESCE(p.refunded_at,CURRENT_TIMESTAMP),p.refund_reason=COALESCE(p.refund_reason,'系统一致性修复')"
            + " WHERE p.status=2 AND a.status IN (6,7,8,9)");
  }
}
