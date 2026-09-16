package io.github.tissyboxc.harmsys.payment;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 处理模拟支付、退款和支付状态查询的业务服务。 */
public class PaymentService {
  private final JdbcTemplate jdbc;

  public PaymentService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> get(long id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = one("SELECT * FROM payment_record WHERE id=?", id);
    if (p == null) throw new UserRegistrationException(404, "支付记录不存在");
    if (!isAdmin(u) && !Objects.equals(u.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该支付记录");
    return p;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> pay(long appointmentId, HttpServletRequest r) {
    AuthenticatedUser u = requirePatient(r);
    Map<String, Object> a =
        one(
            "SELECT * FROM appointment WHERE id=? AND patient_id=? FOR UPDATE",
            appointmentId,
            u.patient_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    Map<String, Object> p =
        one(
            "SELECT * FROM payment_record WHERE appointment_id=? ORDER BY id DESC LIMIT 1",
            appointmentId);
    if (p != null && ((Number) p.get("status")).intValue() == 2) return p;
    if (((Number) a.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "预约当前不需要支付");
    String no =
        "PAY"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?, ?,1,2,?,CURRENT_TIMESTAMP)",
        no,
        appointmentId,
        u.patient_id(),
        a.get("fee"),
        "MOCK-" + no);
    jdbc.update("UPDATE appointment SET status=2 WHERE id=?", appointmentId);
    log(
        u.user_id(),
        "PAY_APPOINTMENT",
        "payment_record",
        appointmentId,
        "模拟支付成功",
        r.getRemoteAddr());
    notify(((Number) a.get("patient_id")).longValue(), "支付成功", "预约支付已完成", "PAYMENT_SUCCESS");
    return one("SELECT * FROM payment_record WHERE payment_no=?", no);
  }

  @Transactional(rollbackFor = Exception.class)
  public void refund(long paymentId, HttpServletRequest r) {
    refund(paymentId, r, "支付退款");
  }

  @Transactional(rollbackFor = Exception.class)
  public void refund(long paymentId, HttpServletRequest r, String reason) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = one("SELECT * FROM payment_record WHERE id=? FOR UPDATE", paymentId);
    if (p == null) throw new UserRegistrationException(404, "支付记录不存在");
    if (!isAdmin(u) && !Objects.equals(u.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权操作该支付记录");
    if (((Number) p.get("status")).intValue() != 2)
      throw new UserRegistrationException(409, "支付记录当前不能退款");
    long appointmentId = ((Number) p.get("appointment_id")).longValue();
    Map<String, Object> a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", appointmentId);
    if (a == null) throw new UserRegistrationException(404, "关联预约不存在");
    int appointmentStatus = ((Number) a.get("status")).intValue();
    if (appointmentStatus < 1 || appointmentStatus > 4)
      throw new UserRegistrationException(409, "该预约当前不能退款");
    long scheduleId = ((Number) a.get("schedule_id")).longValue();
    one("SELECT id FROM doctor_schedule WHERE id=? FOR UPDATE", scheduleId);
    Object slot = a.get("slot_id");
    if (slot != null)
      one("SELECT id FROM schedule_slot WHERE id=? FOR UPDATE", ((Number) slot).longValue());
    jdbc.update(
        "UPDATE payment_record SET"
            + " status=5,refunded_at=CURRENT_TIMESTAMP,refund_reason=?,refund_operator_id=? WHERE"
            + " id=?",
        reason,
        u.user_id(),
        paymentId);
    jdbc.update(
        "UPDATE appointment SET status=9,cancel_reason=? WHERE id=?", reason, appointmentId);
    if (slot != null)
      jdbc.update(
          "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", ((Number) slot).longValue());
    jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
    log(u.user_id(), "REFUND_PAYMENT", "payment_record", paymentId, "模拟退款成功", r.getRemoteAddr());
    notify(((Number) p.get("patient_id")).longValue(), "退款成功", "预约退款已完成", "PAYMENT_REFUNDED");
  }

  private Map<String, Object> one(String sql, Object... a) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, a);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }

  private void log(long uid, String type, String target, long id, String d, String ip) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        target,
        id,
        d,
        ip);
  }

  private void notify(long uid, String title, String content, String type) {
    jdbc.update(
        "INSERT INTO system_notification(user_id,title,content,notification_type) VALUES(?,?,?,?)",
        uid,
        title,
        content,
        type);
    String recipient =
        jdbc.query(
            "SELECT phone FROM patient WHERE user_id=? LIMIT 1",
            rs -> rs.next() ? rs.getString(1) : null,
            uid);
    if (recipient != null && !recipient.isBlank())
      jdbc.update(
          "INSERT INTO notification_outbox(user_id,channel,recipient,subject,content,status)"
              + " VALUES(?,?,?,?,?,0)",
          uid,
          "SMS",
          recipient,
          title,
          content);
  }

  private boolean isAdmin(AuthenticatedUser u) {
    return u.role_codes().stream().anyMatch(x -> x.equalsIgnoreCase("ADMIN"));
  }

  private AuthenticatedUser requirePatient(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return u;
  }
}
