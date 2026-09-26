package io.github.tissyboxc.harmsys.doctor.service.impl;

import io.github.tissyboxc.harmsys.doctor.service.ScheduleLifecycleService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.doctor.mapper.ScheduleLifecycleMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 排班停诊、状态变更和时间段维护业务逻辑。 */
@Service
public class ScheduleLifecycleServiceImpl implements ScheduleLifecycleService {
  private final ScheduleLifecycleMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public ScheduleLifecycleServiceImpl(
      ScheduleLifecycleMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> adminStop(
      AuthenticatedUser operator, long id, String reason, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> schedule = mapper.lockSchedule(id);
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在");
    return stopLocked(id, normalizeReason(reason, "管理员停诊"), operator.user_id(), ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> doctorStop(
      AuthenticatedUser operator, long id, String reason, String ipAddress) {
    requireDoctor(operator);
    Map<String, Object> schedule =
        mapper.lockDoctorSchedule(id, operator.doctor_id());
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return stopLocked(id, normalizeReason(reason, "医生停诊"), operator.user_id(), ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateStatus(
      AuthenticatedUser operator, long id, int status, String ipAddress) {
    requireAdmin(operator);
    if (status < 0 || status > 3) throw new UserRegistrationException(422, "排班状态只能为0到3");
    if (mapper.updateScheduleStatus(id, status) != 1)
      throw new UserRegistrationException(404, "排班不存在");
    writeLog(
        operator.user_id(),
        "ADMIN_UPDATE_SCHEDULE_STATUS",
        "doctor_schedule",
        id,
        "管理员修改排班状态",
        ipAddress);
    return mapper.findSchedule(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateSlot(
      AuthenticatedUser operator, long slotId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> slot = mapper.findSlot(slotId);
    if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
    int status =
        body.get("status") == null
            ? ((Number) slot.get("status")).intValue()
            : Integer.parseInt(String.valueOf(body.get("status")));
    if (status < 0 || status > 2) throw new UserRegistrationException(422, "时间段状态只能为0、1、2");
    if (status != 0 && mapper.countBlockingAppointments(slotId) > 0)
      throw new UserRegistrationException(409, "已有预约的时间段不能锁定或修改");
    mapper.updateSlotStatus(slotId, status);
    writeLog(
        operator.user_id(),
        "ADMIN_UPDATE_SLOT",
        "schedule_slot",
        slotId,
        "管理员修改时间段状态",
        ipAddress);
    return mapper.findSlot(slotId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteSlot(AuthenticatedUser operator, long slotId, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> slot = mapper.findSlot(slotId);
    if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
    if (((Number) slot.get("status")).intValue() != 0
        || mapper.countBlockingAppointments(slotId) > 0)
      throw new UserRegistrationException(409, "已有预约的时间段不能删除");
    mapper.deleteSlot(slotId);
    writeLog(
        operator.user_id(),
        "ADMIN_DELETE_SLOT",
        "schedule_slot",
        slotId,
        "管理员删除时间段",
        ipAddress);
  }

  private Map<String, Object> stopLocked(
      long id, String reason, long userId, String ipAddress) {
    mapper.stopSchedule(id, reason);
    List<Map<String, Object>> appointments = mapper.activeAppointments(id);
    for (Map<String, Object> appointment : appointments) {
      long appointmentId = ((Number) appointment.get("id")).longValue();
      mapper.cancelAppointment(appointmentId, reason);
      if (appointment.get("slot_id") != null)
        mapper.releaseSlot(((Number) appointment.get("slot_id")).longValue());
      mapper.refundSuccessfulPayment(appointmentId);
    }
    mapper.resetBookedCount(id);
    writeLog(
        userId, "STOP_SCHEDULE", "doctor_schedule", id, "停诊并批量取消预约", ipAddress);
    return mapper.findSchedule(id);
  }

  private void writeLog(
      long userId, String type, String target, long id, String description, String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType(target);
    log.setTargetId(id);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private String normalizeReason(String reason, String defaultReason) {
    return reason == null || reason.isBlank() ? defaultReason : reason;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以停诊");
  }

  private void requireDoctor(AuthenticatedUser user) {
    if (user.doctor_id() == null
        || user.role_codes().stream().noneMatch("DOCTOR"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
  }
}
