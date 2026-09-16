package io.github.tissyboxc.harmsys.doctor;

import io.github.tissyboxc.harmsys.doctor.dto.*;
import io.github.tissyboxc.harmsys.users.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 医生资料、排班和时间段维护的业务服务。 */
public class DoctorService {
  private final DoctorRepository repository;
  private final PermissionAuthorizationService authorizationService;

  public DoctorService(
      DoctorRepository repository, PermissionAuthorizationService authorizationService) {
    this.repository = repository;
    this.authorizationService = authorizationService;
  }

  public DoctorProfileResult profile(HttpServletRequest request) {
    return repository
        .findProfile(currentDoctor(request).doctor_id())
        .orElseThrow(() -> new UserRegistrationException(404, "医生资料不存在"));
  }

  @Transactional(rollbackFor = Exception.class)
  public DoctorProfileResult updateProfile(
      DoctorProfileUpdateRequest update, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    repository.updateProfile(user.doctor_id(), update);
    repository.writeLog(
        user.user_id(),
        "UPDATE_DOCTOR_PROFILE",
        "doctor",
        user.doctor_id(),
        "医生修改个人资料",
        request.getRemoteAddr());
    return profile(request);
  }

  public List<ScheduleResult> schedules(HttpServletRequest request) {
    return repository.findSchedules(currentDoctor(request).doctor_id());
  }

  public List<ScheduleResult> allSchedules(HttpServletRequest request) {
    requireAdmin(request);
    return repository.findAllSchedules();
  }

  public ScheduleResult schedule(long scheduleId) {
    return repository
        .findSchedule(scheduleId)
        .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
  }

  public ScheduleResult adminSchedule(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    return schedule(scheduleId);
  }

  public ScheduleResult scheduleForDoctor(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return schedule(scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult createOwnSchedule(ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = currentDoctor(httpRequest);
    ScheduleRequest actual =
        new ScheduleRequest(
            user.doctor_id(),
            request.department_id(),
            request.schedule_date(),
            request.period(),
            request.start_time(),
            request.end_time(),
            request.total_count(),
            request.fee(),
            request.remark());
    validateSchedule(actual);
    if (!repository.departmentMatchesDoctor(user.doctor_id(), actual.department_id()))
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
    try {
      long id = repository.insertSchedule(actual);
      repository.writeLog(
          user.user_id(),
          "CREATE_OWN_SCHEDULE",
          "doctor_schedule",
          id,
          "医生创建本人排班",
          httpRequest.getRemoteAddr());
      return repository.findSchedule(id).orElseThrow();
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已存在排班");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult createAdminSchedule(
      ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireAdmin(httpRequest);
    validateSchedule(request);
    if (!repository.departmentMatchesDoctor(request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    try {
      long id = repository.insertSchedule(request);
      repository.writeLog(
          user.user_id(),
          "ADMIN_CREATE_SCHEDULE",
          "doctor_schedule",
          id,
          "管理员创建排班",
          httpRequest.getRemoteAddr());
      return repository.findSchedule(id).orElseThrow();
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已存在排班");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult updateOwnSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = currentDoctor(httpRequest);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    ScheduleRequest actual =
        new ScheduleRequest(
            user.doctor_id(),
            request.department_id(),
            request.schedule_date(),
            request.period(),
            request.start_time(),
            request.end_time(),
            request.total_count(),
            request.fee(),
            request.remark());
    validateSchedule(actual);
    if (!repository.departmentMatchesDoctor(user.doctor_id(), actual.department_id()))
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
    repository.updateSchedule(scheduleId, actual);
    repository.writeLog(
        user.user_id(),
        "UPDATE_OWN_SCHEDULE",
        "doctor_schedule",
        scheduleId,
        "医生修改本人排班",
        httpRequest.getRemoteAddr());
    return repository.findSchedule(scheduleId).orElseThrow();
  }

  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult updateAdminSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireAdmin(httpRequest);
    if (repository.findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    validateSchedule(request);
    if (!repository.departmentMatchesDoctor(request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    repository.updateSchedule(scheduleId, request);
    repository.writeLog(
        user.user_id(),
        "ADMIN_UPDATE_SCHEDULE",
        "doctor_schedule",
        scheduleId,
        "管理员修改排班",
        httpRequest.getRemoteAddr());
    return repository.findSchedule(scheduleId).orElseThrow();
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteOwnSchedule(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    repository.deleteSchedule(scheduleId);
    repository.writeLog(
        user.user_id(),
        "DELETE_OWN_SCHEDULE",
        "doctor_schedule",
        scheduleId,
        "医生删除本人排班",
        request.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteAdminSchedule(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (repository.findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    repository.deleteSchedule(scheduleId);
    repository.writeLog(
        user.user_id(),
        "ADMIN_DELETE_SCHEDULE",
        "doctor_schedule",
        scheduleId,
        "管理员删除排班",
        request.getRemoteAddr());
  }

  public List<SlotResult> adminSlots(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    if (repository.findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    return repository.findSlots(scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public SlotResult createAdminSlot(long scheduleId, SlotRequest slot, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (repository.findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    validateSlot(scheduleId, slot);
    try {
      long id = repository.insertSlot(scheduleId, slot);
      repository.writeLog(
          user.user_id(),
          "ADMIN_CREATE_SLOT",
          "schedule_slot",
          id,
          "管理员创建排班时间段",
          request.getRemoteAddr());
      return repository.findSlot(id).orElseThrow();
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void adminUpdateSlotStatus(long slotId, int status, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (status < 0 || status > 2) throw new UserRegistrationException(422, "时间段状态只能是0、1、2");
    repository.updateSlotStatus(slotId, status);
    repository.writeLog(
        user.user_id(),
        "ADMIN_UPDATE_SLOT_STATUS",
        "schedule_slot",
        slotId,
        "管理员修改时间段状态",
        request.getRemoteAddr());
  }

  public List<SlotResult> slots(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return repository.findSlots(scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public SlotResult createOwnSlot(long scheduleId, SlotRequest slot, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    validateSlot(scheduleId, slot);
    try {
      long id = repository.insertSlot(scheduleId, slot);
      repository.writeLog(
          user.user_id(),
          "CREATE_OWN_SLOT",
          "schedule_slot",
          id,
          "医生创建排班时间段",
          request.getRemoteAddr());
      return repository.findSlot(id).orElseThrow();
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateSlotStatus(long slotId, int status, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    SlotResult slot =
        repository.findSlot(slotId).orElseThrow(() -> new UserRegistrationException(404, "时间段不存在"));
    if (!repository.scheduleBelongsTo(slot.schedule_id(), user.doctor_id()))
      throw new UserRegistrationException(403, "不能管理其他医生的时间段");
    if (status < 0 || status > 2) throw new UserRegistrationException(422, "时间段状态只能是0、1、2");
    repository.updateSlotStatus(slotId, status);
    repository.writeLog(
        user.user_id(),
        "UPDATE_SLOT_STATUS",
        "schedule_slot",
        slotId,
        "医生修改时间段状态",
        request.getRemoteAddr());
  }

  private void validateSchedule(ScheduleRequest r) {
    if (r.doctor_id() == null) throw new UserRegistrationException(422, "必须指定医生");
    if (r.schedule_date().isBefore(LocalDate.now()))
      throw new UserRegistrationException(422, "排班日期不能早于当前日期");
    if (!r.start_time().isBefore(r.end_time()))
      throw new UserRegistrationException(422, "排班开始时间必须早于结束时间");
    if (!repository.doctorEnabled(r.doctor_id()))
      throw new UserRegistrationException(422, "医生不存在或已停诊");
  }

  private void validateSlot(long scheduleId, SlotRequest r) {
    ScheduleResult s =
        repository
            .findSchedule(scheduleId)
            .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
    if (!r.start_time().isBefore(r.end_time())
        || r.start_time().isBefore(s.start_time())
        || r.end_time().isAfter(s.end_time()))
      throw new UserRegistrationException(422, "时间段必须位于排班时间范围内");
    if (repository.slotNumberExists(scheduleId, r.slot_no())
        || repository.slotOverlaps(scheduleId, r.start_time(), r.end_time()))
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
  }

  private AuthenticatedUser currentDoctor(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(r -> r.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  private AuthenticatedUser requireAdmin(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.role_codes().stream().noneMatch(r -> r.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return u;
  }
}
