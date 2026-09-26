package io.github.tissyboxc.harmsys.registration.service.impl;

import io.github.tissyboxc.harmsys.registration.service.RegistrationOperationsService;

import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.registration.mapper.RegistrationOperationsMapper;
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
public class RegistrationOperationsServiceImpl implements RegistrationOperationsService {
  private final RegistrationOperationsMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public RegistrationOperationsServiceImpl(
      RegistrationOperationsMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> patients(
      AuthenticatedUser operator, String keyword) {
    requireRegistration(operator);
    return mapper.selectPatients(keyword == null ? null : keyword.trim());
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> patient(AuthenticatedUser operator, long id) {
    requireRegistration(operator);
    Map<String, Object> patient = mapper.selectPatient(id);
    if (patient == null) throw new UserRegistrationException(404, "患者不存在");
    return patient;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> create(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireRegistration(operator);
    long patientId = number(body, "patient_id");
    long scheduleId = number(body, "schedule_id");
    Long slotId =
        body.get("slot_id") == null ? null : Long.parseLong(String.valueOf(body.get("slot_id")));
    if (mapper.countActivePatient(patientId) == 0)
      throw new UserRegistrationException(404, "患者不存在");
    Map<String, Object> schedule = mapper.lockSchedule(scheduleId);
    if (schedule == null || ((Number) schedule.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "排班不可预约");
    if (((Number) schedule.get("booked_count")).intValue()
        >= ((Number) schedule.get("total_count")).intValue())
      throw new UserRegistrationException(409, "号源已满");
    if (slotId != null && mapper.reserveSlot(slotId, scheduleId) != 1)
      throw new UserRegistrationException(409, "时间段不可用");

    Integer nextQueueNo = mapper.selectNextQueueNo(scheduleId);
    int queueNo = nextQueueNo == null ? 1 : nextQueueNo;
    String appointmentNo = newNumber("A", 6);
    mapper.insertAppointment(
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
        body.get("remark") == null ? null : String.valueOf(body.get("remark")));
    Long appointmentId = mapper.selectAppointmentIdByNo(appointmentNo);
    if (appointmentId == null) throw new IllegalStateException("创建预约失败");
    mapper.incrementBookedCount(scheduleId);
    mapper.insertSuccessfulPayment(
        newNumber("PAY", 8), appointmentId, patientId, (BigDecimal) schedule.get("fee"));
    log(operator.user_id(), "REGISTRATION_CREATE_APPOINTMENT", appointmentId,
        "挂号员代患者挂号", ipAddress);
    return mapper.selectAppointmentDetail(appointmentId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void refund(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    Map<String, Object> appointment = mapper.lockAppointment(id);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在");
    if (mapper.refundPayment(id) != 1)
      throw new UserRegistrationException(409, "没有可退款的支付记录");
    mapper.markRefundedAppointment(id);
    if (appointment.get("slot_id") != null)
      mapper.releaseSlot(((Number) appointment.get("slot_id")).longValue());
    mapper.decrementBookedCount(((Number) appointment.get("schedule_id")).longValue());
    log(operator.user_id(), "REGISTRATION_REFUND_APPOINTMENT", id, "挂号员退号并退款", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> callNext(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (mapper.callNext(id) != 1)
      throw new UserRegistrationException(409, "该预约不在候诊队列");
    mapper.startVisitForAppointment(id);
    log(operator.user_id(), "REGISTRATION_CALL_NEXT", id, "挂号员呼叫下一位患者", ipAddress);
    return mapper.selectAppointment(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void noShow(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (mapper.markNoShow(id) != 1)
      throw new UserRegistrationException(409, "预约不在候诊队列");
    log(operator.user_id(), "REGISTRATION_MARK_NO_SHOW", id, "标记患者过号", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> requeue(AuthenticatedUser operator, long id, String ipAddress) {
    requireRegistration(operator);
    if (mapper.requeue(id) != 1)
      throw new UserRegistrationException(409, "该预约不能重新排队");
    log(operator.user_id(), "REGISTRATION_REQUEUE", id, "患者重新排队", ipAddress);
    return mapper.selectAppointment(id);
  }

  private void log(
      long userId, String operationType, long appointmentId, String description, String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(operationType);
    log.setTargetType("appointment");
    log.setTargetId(appointmentId);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
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

