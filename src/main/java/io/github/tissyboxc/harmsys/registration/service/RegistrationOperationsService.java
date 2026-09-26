package io.github.tissyboxc.harmsys.registration.service;

import io.github.tissyboxc.harmsys.registration.repository.RegistrationOperationsRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 挂号员工作台业务逻辑。 */
@Service
public class RegistrationOperationsService {
  private final RegistrationOperationsRepository repository;

  public RegistrationOperationsService(RegistrationOperationsRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> patients(
      AuthenticatedUser operator, String keyword) {
    requireRegistration(operator);
    return repository.patients(keyword);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> patient(AuthenticatedUser operator, long id) {
    requireRegistration(operator);
    Map<String, Object> patient = repository.patient(id);
    if (patient == null) throw new UserRegistrationException(404, "患者不存在");
    return patient;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireRegistration(operator);
    long patientId = number(body, "patient_id");
    long scheduleId = number(body, "schedule_id");
    Long slotId =
        body.get("slot_id") == null ? null : Long.parseLong(String.valueOf(body.get("slot_id")));
    if (!repository.patientExists(patientId))
      throw new UserRegistrationException(404, "患者不存在");
    Map<String, Object> schedule = repository.lockSchedule(scheduleId);
    if (schedule == null || ((Number) schedule.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "排班不可预约");
    if (((Number) schedule.get("booked_count")).intValue()
        >= ((Number) schedule.get("total_count")).intValue())
      throw new UserRegistrationException(409, "号源已满");
    if (slotId != null && repository.reserveSlot(slotId, scheduleId) != 1)
      throw new UserRegistrationException(409, "时间段不可用");

    int queueNo = repository.nextQueueNo(scheduleId);
    String appointmentNo = newNumber("A", 6);
    long appointmentId =
        repository.insertAppointment(
            appointmentNo, patientId, schedule, scheduleId, slotId, queueNo,
            body.get("remark") == null ? null : String.valueOf(body.get("remark")));
    repository.incrementBookedCount(scheduleId);
    repository.insertSuccessfulPayment(
        newNumber("PAY", 8), appointmentId, patientId, (BigDecimal) schedule.get("fee"));
    repository.insertOperationLog(
        operator.user_id(),
        "REGISTRATION_CREATE_APPOINTMENT",
        appointmentId,
        "挂号员代患者挂号",
        ipAddress);
    return repository.appointmentDetail(appointmentId);
  }

  @Transactional(rollbackFor = Exception.class)
  public void refund(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    Map<String, Object> appointment = repository.lockAppointment(id);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在");
    if (repository.refundPayment(id) != 1)
      throw new UserRegistrationException(409, "没有可退款的支付记录");
    repository.markRefundedAppointment(id);
    if (appointment.get("slot_id") != null)
      repository.releaseSlot(((Number) appointment.get("slot_id")).longValue());
    repository.decrementBookedCount(((Number) appointment.get("schedule_id")).longValue());
    repository.insertOperationLog(
        operator.user_id(), "REGISTRATION_REFUND_APPOINTMENT", id, "挂号员退号并退款", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> callNext(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (repository.callNext(id) != 1)
      throw new UserRegistrationException(409, "该预约不在候诊队列");
    repository.startVisitForAppointment(id);
    repository.insertOperationLog(
        operator.user_id(), "REGISTRATION_CALL_NEXT", id, "挂号员呼叫下一位患者", ipAddress);
    return repository.appointment(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void noShow(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (repository.markNoShow(id) != 1)
      throw new UserRegistrationException(409, "预约不在候诊队列");
    repository.insertOperationLog(
        operator.user_id(), "REGISTRATION_MARK_NO_SHOW", id, "标记患者过号", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> requeue(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (repository.requeue(id) != 1)
      throw new UserRegistrationException(409, "该预约不能重新排队");
    repository.insertOperationLog(
        operator.user_id(), "REGISTRATION_REQUEUE", id, "患者重新排队", ipAddress);
    return repository.appointment(id);
  }

  private String newNumber(String prefix, int randomLength) {
    return prefix
        + System.currentTimeMillis()
        + UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, randomLength)
            .toUpperCase(Locale.ROOT);
  }

  private long number(Map<String, Object> body, String key) {
    if (body.get(key) == null) throw new UserRegistrationException(422, key + "不能为空");
    return Long.parseLong(String.valueOf(body.get(key)));
  }

  private void requireRegistration(AuthenticatedUser user) {
    if (user.role_codes().stream()
        .noneMatch(x -> x.equalsIgnoreCase("REGISTRATION") || x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "没有挂号员权限");
  }
}

