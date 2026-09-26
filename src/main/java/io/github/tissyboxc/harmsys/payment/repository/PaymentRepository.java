package io.github.tissyboxc.harmsys.payment.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 支付、退款及相关预约状态变更的数据访问层。 */
@Repository
public class PaymentRepository {
  private final JdbcTemplate jdbc;

  public PaymentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> findPayment(long paymentId) {
    return one("SELECT * FROM payment_record WHERE id=?", paymentId);
  }

  public Map<String, Object> lockPayment(long paymentId) {
    return one("SELECT * FROM payment_record WHERE id=? FOR UPDATE", paymentId);
  }

  public Map<String, Object> lockAppointmentForPatient(long appointmentId, long patientId) {
    return one(
        "SELECT * FROM appointment WHERE id=? AND patient_id=? FOR UPDATE",
        appointmentId,
        patientId);
  }

  public Map<String, Object> lockAppointment(long appointmentId) {
    return one("SELECT * FROM appointment WHERE id=? FOR UPDATE", appointmentId);
  }

  public Map<String, Object> latestAppointmentPayment(long appointmentId) {
    return one(
        "SELECT * FROM payment_record WHERE appointment_id=? ORDER BY id DESC LIMIT 1",
        appointmentId);
  }

  public int insertPayment(
      String paymentNo, long appointmentId, long patientId, Object amount, String thirdPartyNo) {
    return jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?, ?,1,2,?,CURRENT_TIMESTAMP)",
        paymentNo,
        appointmentId,
        patientId,
        amount,
        thirdPartyNo);
  }

  public Map<String, Object> findPaymentByNo(String paymentNo) {
    return one("SELECT * FROM payment_record WHERE payment_no=?", paymentNo);
  }

  public int markAppointmentPaid(long appointmentId) {
    return jdbc.update("UPDATE appointment SET status=2 WHERE id=?", appointmentId);
  }

  public void lockSchedule(long scheduleId) {
    one("SELECT id FROM doctor_schedule WHERE id=? FOR UPDATE", scheduleId);
  }

  public void lockSlot(long slotId) {
    one("SELECT id FROM schedule_slot WHERE id=? FOR UPDATE", slotId);
  }

  public int markPaymentRefunded(long paymentId, String reason, long operatorId) {
    return jdbc.update(
        "UPDATE payment_record SET"
            + " status=5,refunded_at=CURRENT_TIMESTAMP,refund_reason=?,refund_operator_id=? WHERE"
            + " id=?",
        reason,
        operatorId,
        paymentId);
  }

  public int cancelAppointment(long appointmentId, String reason) {
    return jdbc.update(
        "UPDATE appointment SET status=9,cancel_reason=? WHERE id=?", reason, appointmentId);
  }

  public int releaseSlot(long slotId) {
    return jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", slotId);
  }

  public int decrementBookedCount(long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
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

  public Long patientUserId(long patientId) {
    return jdbc.query(
        "SELECT user_id FROM patient WHERE id=? AND deleted=0",
        rs -> rs.next() ? rs.getLong(1) : null,
        patientId);
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

  public String patientPhone(long patientId) {
    return jdbc.query(
        "SELECT phone FROM patient WHERE id=? LIMIT 1",
        rs -> rs.next() ? rs.getString(1) : null,
        patientId);
  }

  public void insertNotificationOutbox(
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
