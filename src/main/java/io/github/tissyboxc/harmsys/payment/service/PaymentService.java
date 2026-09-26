package io.github.tissyboxc.harmsys.payment.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.payment.repository.PaymentRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 处理模拟支付、退款和支付状态查询的业务服务。 */
public class PaymentService {
  private final PaymentRepository repository;

  public PaymentService(PaymentRepository repository) {
    this.repository = repository;
  }

  /**
   * 支付记录查询逻辑
   * @param id 订单号
   */
  public Map<String, Object> get(long id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = repository.findPayment(id);
    if (p == null) throw new UserRegistrationException(404, "支付记录不存在");
    if (!isAdmin(u) && !Objects.equals(u.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该支付记录");
    return p;
  }

  /**
   *统一支付接口
   *
   * @param appointmentId 预约ID
   */
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> pay(long appointmentId, HttpServletRequest r) {
    //验证登录/患者身份
    AuthenticatedUser u = requirePatient(r);
    //查询预约信息
    Map<String, Object> a =
        repository.lockAppointmentForPatient(appointmentId, u.patient_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    //查询当前支付状态
    Map<String, Object> p = repository.latestAppointmentPayment(appointmentId);
    //支付成功
    if (p != null && ((Number) p.get("status")).intValue() == 2) return p;
    //订单状态处于非"待支付"状态
    if (((Number) a.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "预约当前不需要支付");
    //支付单号生成
    String no =
        "PAY"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    repository.insertPayment(
        no,
        appointmentId,
        u.patient_id(),
        a.get("fee"),
        "MOCK-" + no);
    repository.markAppointmentPaid(appointmentId);
    log(
        u.user_id(),
        "PAY_APPOINTMENT",
        "payment_record",
        appointmentId,
        "模拟支付成功",
        r.getRemoteAddr());
    notifyPatient(
        ((Number) a.get("patient_id")).longValue(), "支付成功", "预约支付已完成", "PAYMENT_SUCCESS");
    return repository.findPaymentByNo(no);
  }

  /**
   * 支付退款
   * @param paymentId 订单ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void refund(long paymentId, HttpServletRequest r) {
    refund(paymentId, r, "支付退款");
  }

  /**
   * 退款统一接口
   *
   * @param paymentId 订单iD
   * @param reason 退款原因
   */
  @Transactional(rollbackFor = Exception.class)
  public void refund(long paymentId, HttpServletRequest r, String reason) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = repository.lockPayment(paymentId);
    if (p == null) throw new UserRegistrationException(404, "支付记录不存在");
    if (!isAdmin(u) && !Objects.equals(u.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权操作该支付记录");
    if (((Number) p.get("status")).intValue() != 2)
      throw new UserRegistrationException(409, "支付记录当前不能退款");
    long appointmentId = ((Number) p.get("appointment_id")).longValue();
    Map<String, Object> a = repository.lockAppointment(appointmentId);
    if (a == null) throw new UserRegistrationException(404, "关联预约不存在");
    int appointmentStatus = ((Number) a.get("status")).intValue();
    if (appointmentStatus < 1 || appointmentStatus > 4)
      throw new UserRegistrationException(409, "该预约当前不能退款");
    long scheduleId = ((Number) a.get("schedule_id")).longValue();
    repository.lockSchedule(scheduleId);
    Object slot = a.get("slot_id");
    if (slot != null) repository.lockSlot(((Number) slot).longValue());
    repository.markPaymentRefunded(paymentId, reason, u.user_id());
    repository.cancelAppointment(appointmentId, reason);
    if (slot != null) repository.releaseSlot(((Number) slot).longValue());
    repository.decrementBookedCount(scheduleId);
    log(u.user_id(), "REFUND_PAYMENT", "payment_record", paymentId, "模拟退款成功", r.getRemoteAddr());
    notifyPatient(
        ((Number) p.get("patient_id")).longValue(), "退款成功", "预约退款已完成", "PAYMENT_REFUNDED");
  }

  private void log(long uid, String type, String target, long id, String d, String ip) {
    repository.insertOperationLog(uid, type, target, id, d, ip);
  }

  private void notifyPatient(long patientId, String title, String content, String type) {
    Long userId = repository.patientUserId(patientId);
    if (userId == null) throw new UserRegistrationException(404, "患者账号不存在");

    repository.insertNotification(userId, title, content, type);
    String recipient = repository.patientPhone(patientId);
    if (recipient != null && !recipient.isBlank())
      repository.insertNotificationOutbox(userId, "SMS", recipient, title, content);
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


