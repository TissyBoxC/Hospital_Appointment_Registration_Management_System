package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.appointment.repository.AdminAppointmentRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员预约修改、迁移、取消与过期业务逻辑。 */
@Service
public class AdminAppointmentOperationsService {
  private final AdminAppointmentRepository repository;

  public AdminAppointmentOperationsService(AdminAppointmentRepository repository) {
    this.repository = repository;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> appointment = repository.lockAppointment(id);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在");

    if (body.containsKey("schedule_id") || body.containsKey("slot_id")) {
      migrate(id, appointment, body, operator.user_id(), ipAddress);
      appointment = repository.lockAppointment(id);
    }

    String remark =
        body.get("remark") == null
            ? (String) appointment.get("remark")
            : String.valueOf(body.get("remark"));
    int status =
        body.get("status") == null
            ? ((Number) appointment.get("status")).intValue()
            : Integer.parseInt(String.valueOf(body.get("status")));
    Integer queueNo =
        body.get("queue_no") == null
            ? (Integer) appointment.get("queue_no")
            : Integer.valueOf(String.valueOf(body.get("queue_no")));
    if (status < 1 || status > 9) throw new UserRegistrationException(422, "预约状态必须为1到9");

    int oldStatus = ((Number) appointment.get("status")).intValue();
    if (isActive(oldStatus) && !isActive(status)) {
      release(appointment);
      if (status >= 6) repository.refundSuccessfulPayment(id);
    } else if (!isActive(oldStatus) && isActive(status)) {
      restore(appointment);
    }

    repository.updateAppointment(id, queueNo, status, remark);
    repository.insertOperationLog(
        operator.user_id(), "ADMIN_UPDATE_APPOINTMENT", id, "管理员修改预约", ipAddress);
    return repository.detail(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void cancel(
      AuthenticatedUser operator, long id, String reason, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> appointment = repository.lockAppointment(id);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在");
    if (!isActive(((Number) appointment.get("status")).intValue()))
      throw new UserRegistrationException(409, "当前预约不能取消");
    String normalizedReason = reason == null || reason.isBlank() ? "管理员取消预约" : reason;
    repository.cancel(id, normalizedReason);
    release(appointment);
    repository.refundSuccessfulPayment(id, normalizedReason);
    repository.insertOperationLog(
        operator.user_id(), "ADMIN_CANCEL_APPOINTMENT", id, normalizedReason, ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public void expire(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    Map<String, Object> appointment = repository.lockAppointment(id);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在");
    if (repository.expire(id) != 1)
      throw new UserRegistrationException(409, "预约当前不能标记过期");
    release(appointment);
    repository.insertOperationLog(
        operator.user_id(), "ADMIN_EXPIRE_APPOINTMENT", id, "管理员标记预约过期", ipAddress);
  }

  private void migrate(
      long id,
      Map<String, Object> current,
      Map<String, Object> body,
      long operatorId,
      String ipAddress) {
    int currentStatus = ((Number) current.get("status")).intValue();
    if (!isActive(currentStatus))
      throw new UserRegistrationException(409, "只有有效预约可以迁移");

    long oldSchedule = ((Number) current.get("schedule_id")).longValue();
    long targetSchedule =
        body.get("schedule_id") == null
            ? oldSchedule
            : Long.parseLong(String.valueOf(body.get("schedule_id")));
    Long oldSlot =
        current.get("slot_id") == null ? null : ((Number) current.get("slot_id")).longValue();
    Long targetSlot;
    if (body.containsKey("slot_id")) {
      targetSlot =
          body.get("slot_id") == null
              ? null
              : Long.parseLong(String.valueOf(body.get("slot_id")));
    } else {
      targetSlot = targetSchedule == oldSchedule ? oldSlot : null;
    }

    Map<String, Object> schedule = repository.lockSchedule(targetSchedule);
    if (schedule == null
        || ((Number) schedule.get("status")).intValue() == 2
        || ((Number) schedule.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "目标排班不存在或不可预约");
    if (schedule.get("schedule_date") instanceof java.sql.Date date
        && date.toLocalDate().isBefore(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "目标排班日期已过");

    if (targetSchedule != oldSchedule
        && repository.countActiveForPatientAndSchedule(
                ((Number) current.get("patient_id")).longValue(), targetSchedule, id)
            > 0)
      throw new UserRegistrationException(409, "患者已预约目标排班");
    if (targetSchedule != oldSchedule
        && ((Number) schedule.get("booked_count")).intValue()
            >= ((Number) schedule.get("total_count")).intValue())
      throw new UserRegistrationException(409, "目标排班号源已满");

    if (targetSlot != null) {
      Map<String, Object> slot = repository.lockSlot(targetSlot, targetSchedule);
      if (slot == null || ((Number) slot.get("status")).intValue() != 0)
        throw new UserRegistrationException(409, "目标时间段不可用");
    }

    if (oldSlot != null && !Objects.equals(oldSlot, targetSlot)) repository.releaseSlot(oldSlot);
    if (targetSlot != null && !Objects.equals(oldSlot, targetSlot))
      repository.reserveSlot(targetSlot);

    if (targetSchedule != oldSchedule) {
      repository.decrementBookedCount(oldSchedule);
      if (repository.incrementBookedCount(targetSchedule) != 1)
        throw new UserRegistrationException(409, "目标排班号源已满");
    }

    repository.migrateAppointment(
        id,
        schedule.get("doctor_id"),
        schedule.get("department_id"),
        targetSchedule,
        targetSlot,
        schedule.get("schedule_date"),
        schedule.get("period"),
        schedule.get("fee"));
    repository.insertOperationLog(
        operatorId,
        "ADMIN_MIGRATE_APPOINTMENT",
        id,
        "管理员迁移预约到排班" + targetSchedule,
        ipAddress);
  }

  private void restore(Map<String, Object> appointment) {
    long scheduleId = ((Number) appointment.get("schedule_id")).longValue();
    Map<String, Object> schedule = repository.lockSchedule(scheduleId);
    if (schedule == null
        || ((Number) schedule.get("status")).intValue() == 2
        || ((Number) schedule.get("status")).intValue() == 3
        || ((Number) schedule.get("booked_count")).intValue()
            >= ((Number) schedule.get("total_count")).intValue())
      throw new UserRegistrationException(409, "无法恢复预约，排班号源不可用");
    if (appointment.get("slot_id") != null
        && repository.reserveSlot(((Number) appointment.get("slot_id")).longValue()) != 1)
      throw new UserRegistrationException(409, "无法恢复预约，时间段不可用");
    repository.incrementBookedCountUnchecked(scheduleId);
  }

  private void release(Map<String, Object> appointment) {
    if (appointment.get("slot_id") != null)
      repository.releaseSlot(((Number) appointment.get("slot_id")).longValue());
    repository.decrementBookedCount(((Number) appointment.get("schedule_id")).longValue());
  }

  private boolean isActive(int status) {
    return status >= 1 && status <= 4;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
  }
}

