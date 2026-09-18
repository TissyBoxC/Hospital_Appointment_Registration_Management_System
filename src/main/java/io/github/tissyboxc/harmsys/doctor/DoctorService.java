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

  /**
   * 获取医生个人资料
   */
  public DoctorProfileResult profile(HttpServletRequest request) {
    return repository
        .findProfile(currentDoctor(request).doctor_id())
        .orElseThrow(() -> new UserRegistrationException(404, "医生资料不存在"));
  }

  /**
   * 医生修改个人信息
   * @param update 下游请求体,包含修改信息
   */
  @Transactional(rollbackFor = Exception.class)
  public DoctorProfileResult updateProfile(
      DoctorProfileUpdateRequest update, HttpServletRequest request) {
    //同时验证已登录和身份
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

  /**
   * 医生获取自己的排班信息
   */
  public List<ScheduleResult> schedules(HttpServletRequest request) {
    return repository.findSchedules(currentDoctor(request).doctor_id());
  }

  /**
   * 查询所有排班信息
   */
  public List<ScheduleResult> allSchedules(HttpServletRequest request) {
    requireAdmin(request);
    return repository.findAllSchedules();
  }

  /**
   * 查询排班信息统一逻辑
   * @param scheduleId 排班ID
   */
  public ScheduleResult schedule(long scheduleId) {
    return repository
        .findSchedule(scheduleId)
        .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
  }

  /**
   *管理员查询所有排班
   * @param scheduleId 排班ID
   * @param request 请求体,用于身份验证
   */
  public ScheduleResult adminSchedule(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    return schedule(scheduleId);
  }

  /**
   * 医生查询自己排班的具体信息
   * @param scheduleId 排班ID
   */
  public ScheduleResult scheduleForDoctor(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return schedule(scheduleId);
  }

  /**
   * 医生创建自己的排班信息
   */
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

  /**
   * 管理员创建医生排班
   */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult createAdminSchedule(
      ScheduleRequest request, HttpServletRequest httpRequest) {
    //身份验证
    AuthenticatedUser user = requireAdmin(httpRequest);
    //排班信息校验
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

  /**
   * 医生修改自己排班信息
   * @param scheduleId 排班ID
   * @param request 修改信息
   */
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

  /**
   * 管理员修改排班信息
   */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult updateAdminSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest) {
    //身份验证
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

  /**
   * 医生删除指定排班信息
   * @param scheduleId 排班ID
   */
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

  /**
   * 删除指定排班信息
   * @param scheduleId 排班ID
   */
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

  /**
   * 管理员获取当前排班的时间段信息
   * @param scheduleId 排班ID
   * @param request 下游请求体,用于身份验证
   */
  public List<SlotResult> adminSlots(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    if (repository.findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    return repository.findSlots(scheduleId);
  }

  /**
   * 管理员创建时间段信息
   * @param scheduleId 排班ID
   * @param slot 包含时间段请求体
   */
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

  /**
   * 管理员修改指定时间段的状态
   * @param slotId 时间段
   * @param status 包含状态的请求体
   */
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

  /**
   * 获取排班的时间段信息
   * @param scheduleId 排班ID
   */
  public List<SlotResult> slots(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!repository.scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return repository.findSlots(scheduleId);
  }

  /**
   * 创建指定排版的时间段
   * @param scheduleId 排班ID
   * @param slot 时间段信息
   */
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

  /**
   * 修改时间段状态
   */
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

  /**
   * 排班信息校验
   * 必须存在doctor_id
   * 日期早于现在
   * 开始时间早于结束时间
   * 医生状态
   * @param r 请求体,包含排班信息
   */
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

  /**
   * 根据请求体获取当前医生ID
   * @param request 当前登录的请求体
   * @return 验证通过的请求体
   */
  private AuthenticatedUser currentDoctor(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(r -> r.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  /**
   * 管理员身份验证
   */
  private AuthenticatedUser requireAdmin(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.role_codes().stream().noneMatch(r -> r.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return u;
  }
}
