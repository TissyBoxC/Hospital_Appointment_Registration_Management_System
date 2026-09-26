package io.github.tissyboxc.harmsys.payment.service.impl;

import io.github.tissyboxc.harmsys.payment.service.PaymentService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.payment.mapper.PaymentMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 处理模拟支付、退款和支付状态查询的业务服务。 */
public class PaymentServiceImpl implements PaymentService {
  private final PaymentMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public PaymentServiceImpl(PaymentMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  /**
   * 支付记录查询逻辑
   * @param id 订单号
   */
  @Override
  public Map<String, Object> get(long id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = mapper.selectById(id) == null ? null : mapper.selectMapById(id);
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
  @Override
  public Map<String, Object> pay(long appointmentId, HttpServletRequest r) {
    //验证登录/患者身份
    AuthenticatedUser u = requirePatient(r);
    //查询预约信息
    Map<String, Object> a =
        mapper.lockAppointmentForPatient(appointmentId, u.patient_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    //查询当前支付状态
    Map<String, Object> p = mapper.selectLatestAppointmentPayment(appointmentId);
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
    mapper.insertSuccessfulPayment(
        no,
        appointmentId,
        u.patient_id(),
        a.get("fee"),
        "MOCK-" + no);
    mapper.markAppointmentPaid(appointmentId);
    log(
        u.user_id(),
        "PAY_APPOINTMENT",
        "payment_record",
        appointmentId,
        "模拟支付成功",
        r.getRemoteAddr());
    notifyPatient(
        ((Number) a.get("patient_id")).longValue(), "支付成功", "预约支付已完成", "PAYMENT_SUCCESS");
    return mapper.selectByPaymentNo(no);
  }

  /**
   * 支付退款
   * @param paymentId 订单ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
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
  @Override
  public void refund(long paymentId, HttpServletRequest r, String reason) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> p = mapper.lockPayment(paymentId);
    if (p == null) throw new UserRegistrationException(404, "支付记录不存在");
    if (!isAdmin(u) && !Objects.equals(u.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权操作该支付记录");
    if (((Number) p.get("status")).intValue() != 2)
      throw new UserRegistrationException(409, "支付记录当前不能退款");
    long appointmentId = ((Number) p.get("appointment_id")).longValue();
    Map<String, Object> a = mapper.lockAppointment(appointmentId);
    if (a == null) throw new UserRegistrationException(404, "关联预约不存在");
    int appointmentStatus = ((Number) a.get("status")).intValue();
    if (appointmentStatus < 1 || appointmentStatus > 4)
      throw new UserRegistrationException(409, "该预约当前不能退款");
    long scheduleId = ((Number) a.get("schedule_id")).longValue();
    mapper.lockSchedule(scheduleId);
    Object slot = a.get("slot_id");
    if (slot != null) mapper.lockSlot(((Number) slot).longValue());
    mapper.markPaymentRefunded(paymentId, reason, u.user_id());
    mapper.cancelAppointment(appointmentId, reason);
    if (slot != null) mapper.releaseSlot(((Number) slot).longValue());
    mapper.decrementBookedCount(scheduleId);
    log(u.user_id(), "REFUND_PAYMENT", "payment_record", paymentId, "模拟退款成功", r.getRemoteAddr());
    notifyPatient(
        ((Number) p.get("patient_id")).longValue(), "退款成功", "预约退款已完成", "PAYMENT_REFUNDED");
  }

  private void log(long uid, String type, String target, long id, String d, String ip) {
    OperationLog log = new OperationLog();
    log.setUserId(uid);
    log.setOperationType(type);
    log.setTargetType(target);
    log.setTargetId(id);
    log.setDescription(d);
    log.setIpAddress(ip);
    operationLogMapper.insert(log);
  }

  private void notifyPatient(long patientId, String title, String content, String type) {
    Long userId = mapper.selectPatientUserId(patientId);
    if (userId == null) throw new UserRegistrationException(404, "患者账号不存在");

    mapper.insertNotification(userId, title, content, type);
    String recipient = mapper.selectPatientPhone(patientId);
    if (recipient != null && !recipient.isBlank())
      mapper.insertNotificationOutbox(userId, "SMS", recipient, title, content);
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


