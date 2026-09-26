package io.github.tissyboxc.harmsys.doctor.service.impl;

import io.github.tissyboxc.harmsys.doctor.service.ScheduleRequestService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestResult;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestSubmit;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.mapper.DoctorScheduleMapper;
import io.github.tissyboxc.harmsys.doctor.mapper.ScheduleRequestMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医生排班申请的提交、查询和审批业务。 */
@Service
public class ScheduleRequestServiceImpl implements ScheduleRequestService {
  private static final int TYPE_CREATE = 1;
  private static final int TYPE_UPDATE = 2;
  private static final int TYPE_DELETE = 3;
  private static final int STATUS_PENDING = 0;
  private static final String DEPARTMENT_MANAGE = "DEPARTMENT_MANAGE";
  private static final String SCHEDULE_REQUEST_REVIEW = "SCHEDULE_REQUEST_REVIEW";

  private final ScheduleRequestMapper requestMapper;
  private final DoctorScheduleMapper scheduleMapper;
  private final PermissionAuthorizationService permissionAuthorizationService;
  private final OperationLogMapper operationLogMapper;

  public ScheduleRequestServiceImpl(
      ScheduleRequestMapper requestMapper,
      DoctorScheduleMapper scheduleMapper,
      PermissionAuthorizationService permissionAuthorizationService,
      OperationLogMapper operationLogMapper) {
    this.requestMapper = requestMapper;
    this.scheduleMapper = scheduleMapper;
    this.permissionAuthorizationService = permissionAuthorizationService;
    this.operationLogMapper = operationLogMapper;
  }

  /** 医生提交新增排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleRequestResult create(
      ScheduleRequestSubmit body, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    validate(body);
    assertDepartmentMatches(doctor.doctor_id(), body.department_id());
    assertNoPendingConflict(doctor.doctor_id(), null, body);
    DoctorScheduleRequest entity =
        toEntity(doctor.doctor_id(), body.department_id(), null, TYPE_CREATE, body, doctor.user_id());
    requestMapper.insert(entity);
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", entity.getId(), "医生提交排班申请", request.getRemoteAddr());
    return one(entity.getId());
  }

  /** 医生提交修改排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleRequestResult update(
      long scheduleId, ScheduleRequestSubmit body, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    validate(body);
    assertDepartmentMatches(doctor.doctor_id(), body.department_id());
    DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
    if (schedule == null || !schedule.getDoctorId().equals(doctor.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    if (schedule.getBookedCount() != null && schedule.getBookedCount() > 0)
      throw new UserRegistrationException(409, "已有预约的排班不能申请修改");
    assertNoPendingConflict(doctor.doctor_id(), scheduleId, body);
    DoctorScheduleRequest entity =
        toEntity(doctor.doctor_id(), body.department_id(), scheduleId, TYPE_UPDATE, body, doctor.user_id());
    requestMapper.insert(entity);
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", entity.getId(), "医生提交排班申请", request.getRemoteAddr());
    return one(entity.getId());
  }

  /** 医生提交删除排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleRequestResult delete(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    DoctorSchedule schedule = scheduleMapper.selectById(scheduleId);
    if (schedule == null || !schedule.getDoctorId().equals(doctor.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    if (schedule.getBookedCount() != null && schedule.getBookedCount() > 0)
      throw new UserRegistrationException(409, "已有预约的排班不能申请删除");
    ScheduleRequestSubmit body =
        new ScheduleRequestSubmit(
            schedule.getDepartmentId(),
            schedule.getScheduleDate(),
            schedule.getPeriod(),
            schedule.getStartTime(),
            schedule.getEndTime(),
            schedule.getTotalCount(),
            schedule.getFee(),
            schedule.getRemark());
    assertNoPendingConflict(doctor.doctor_id(), scheduleId, body);
    DoctorScheduleRequest entity =
        toEntity(
            doctor.doctor_id(),
            schedule.getDepartmentId(),
            scheduleId,
            TYPE_DELETE,
            body,
            doctor.user_id());
    requestMapper.insert(entity);
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", entity.getId(), "医生提交排班申请", request.getRemoteAddr());
    return one(entity.getId());
  }

  /** 医生查看自己的申请。 */
  @Transactional(readOnly = true)
  @Override
  public List<ScheduleRequestResult> mine(Integer status, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    if (status != null) validateStatus(status);
    return requestMapper.selectMine(doctor.doctor_id(), status);
  }

  /** 医生取消自己的待审核申请。 */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleRequestResult cancelMine(long id, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    DoctorScheduleRequest entity = requestMapper.lockDoctorRequest(id, doctor.doctor_id());
    if (entity == null)
      throw new UserRegistrationException(404, "排班申请不存在或不属于当前医生");
    if (entity.getStatus() != STATUS_PENDING)
      throw new UserRegistrationException(409, "只有待审核申请可以取消");
    requestMapper.cancelRequest(id);
    writeLog(
        doctor.user_id(), "CANCEL_SCHEDULE_REQUEST", id, "医生取消排班申请", request.getRemoteAddr());
    return one(id);
  }

  /** 查看当前账号审核范围内的排班申请。 */
  @Transactional(readOnly = true)
  @Override
  public List<ScheduleRequestResult> reviewList(
      Integer status, Long departmentId, HttpServletRequest request) {
    AuthenticatedUser operator = requireReviewer(request);
    boolean admin = isAdmin(operator);
    boolean departmentManager =
        permissionAuthorizationService.hasPermission(operator.user_id(), DEPARTMENT_MANAGE);
    boolean hasReviewPermission =
        permissionAuthorizationService.hasPermission(
            operator.user_id(), SCHEDULE_REQUEST_REVIEW);
    if (status != null) validateStatus(status);
    return requestMapper.selectReviewList(
        admin,
        departmentManager,
        hasReviewPermission,
        operator.department_id(),
        operator.user_id(),
        status,
        departmentId);
  }

  /** 对排班申请执行通过或驳回。 */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleRequestResult review(
      long id, boolean approved, String reviewRemark, HttpServletRequest request) {
    AuthenticatedUser operator = requireReviewer(request);
    DoctorScheduleRequest entity = requestMapper.lockRequest(id);
    if (entity == null) throw new UserRegistrationException(404, "排班申请不存在");
    if (entity.getStatus() != STATUS_PENDING)
      throw new UserRegistrationException(409, "该申请已经处理，不能重复审核");
    assertCanReview(operator, entity.getDepartmentId());
    if (!approved) {
      requestMapper.rejectRequest(id, operator.user_id(), reviewRemark);
      writeLog(
          operator.user_id(),
          "REJECT_SCHEDULE_REQUEST",
          id,
          "驳回医生排班申请",
          request.getRemoteAddr());
      return one(id);
    }

    int type = entity.getRequestType();
    Long scheduleId;
    try {
      if (type == TYPE_CREATE) scheduleId = applyCreate(entity);
      else if (type == TYPE_UPDATE) {
        scheduleId = entity.getTargetScheduleId();
        applyUpdate(scheduleId, entity);
      } else if (type == TYPE_DELETE) {
        scheduleId = entity.getTargetScheduleId();
        applyDelete(scheduleId);
      } else {
        throw new UserRegistrationException(422, "排班申请类型不合法");
      }
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已有正式排班");
    }
    requestMapper.approveRequest(
        id, operator.user_id(), reviewRemark, type == TYPE_DELETE ? null : scheduleId);
    writeLog(
        operator.user_id(),
        "APPROVE_SCHEDULE_REQUEST",
        id,
        "通过医生排班申请",
        request.getRemoteAddr());
    return one(id);
  }

  private long applyCreate(DoctorScheduleRequest entity) {
    DoctorSchedule schedule = toSchedule(entity);
    scheduleMapper.insert(schedule);
    return schedule.getId();
  }

  private void applyUpdate(Long scheduleId, DoctorScheduleRequest entity) {
    if (scheduleId == null) throw new UserRegistrationException(409, "待修改的排班不存在");
    DoctorSchedule schedule = toSchedule(entity);
    schedule.setId(scheduleId);
    if (scheduleMapper.updateWithoutBookings(schedule) != 1)
      throw new UserRegistrationException(409, "排班已有预约，不能执行修改");
  }

  private void applyDelete(Long scheduleId) {
    if (scheduleId == null) throw new UserRegistrationException(409, "待删除的排班不存在");
    if (scheduleMapper.deleteWithoutBookings(scheduleId) != 1)
      throw new UserRegistrationException(409, "排班已有预约，不能执行删除");
  }

  private DoctorSchedule toSchedule(DoctorScheduleRequest entity) {
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDoctorId(entity.getDoctorId());
    schedule.setDepartmentId(entity.getDepartmentId());
    schedule.setScheduleDate(entity.getScheduleDate());
    schedule.setPeriod(entity.getPeriod());
    schedule.setStartTime(entity.getStartTime());
    schedule.setEndTime(entity.getEndTime());
    schedule.setTotalCount(entity.getTotalCount());
    schedule.setBookedCount(0);
    schedule.setFee(entity.getFee());
    schedule.setStatus(0);
    schedule.setRemark(entity.getRemark());
    return schedule;
  }

  private DoctorScheduleRequest toEntity(
      long doctorId,
      long departmentId,
      Long targetScheduleId,
      int type,
      ScheduleRequestSubmit body,
      long requestedByUserId) {
    DoctorScheduleRequest entity = new DoctorScheduleRequest();
    entity.setDoctorId(doctorId);
    entity.setDepartmentId(departmentId);
    entity.setTargetScheduleId(targetScheduleId);
    entity.setRequestType(type);
    entity.setScheduleDate(body.schedule_date());
    entity.setPeriod(body.period());
    entity.setStartTime(body.start_time());
    entity.setEndTime(body.end_time());
    entity.setTotalCount(body.total_count());
    entity.setFee(body.fee());
    entity.setRemark(body.remark());
    entity.setStatus(STATUS_PENDING);
    entity.setRequestedByUserId(requestedByUserId);
    return entity;
  }

  private void validate(ScheduleRequestSubmit body) {
    if (body.schedule_date().isBefore(LocalDate.now()))
      throw new UserRegistrationException(422, "排班日期不能早于当前日期");
    if (!body.start_time().isBefore(body.end_time()))
      throw new UserRegistrationException(422, "排班开始时间必须早于结束时间");
  }

  private void validateStatus(int status) {
    if (status < 0 || status > 3)
      throw new UserRegistrationException(422, "申请状态只能为0、1、2、3");
  }

  private void assertDepartmentMatches(long doctorId, long departmentId) {
    if (scheduleMapper.countDoctorDepartment(doctorId, departmentId) == 0)
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
  }

  private void assertNoPendingConflict(
      long doctorId, Long targetScheduleId, ScheduleRequestSubmit body) {
    if (requestMapper.countPendingConflicts(
            doctorId, targetScheduleId, body.schedule_date(), body.period())
        > 0)
      throw new UserRegistrationException(409, "该日期和时段已有待审核申请");
    if (scheduleMapper.countConflicts(
            doctorId, body.schedule_date(), body.period(), targetScheduleId)
        > 0)
      throw new UserRegistrationException(
          409, targetScheduleId == null ? "该日期和时段已有正式排班" : "该日期和时段已有其他正式排班");
  }

  private void assertCanReview(AuthenticatedUser operator, Long departmentId) {
    if (isAdmin(operator)) return;
    if (permissionAuthorizationService.hasPermission(
            operator.user_id(), DEPARTMENT_MANAGE)
        && requestMapper.countDepartmentManager(operator.user_id(), departmentId) > 0) {
      return;
    }
    boolean hasReviewPermission =
        permissionAuthorizationService.hasPermission(
            operator.user_id(), SCHEDULE_REQUEST_REVIEW);
    if (hasReviewPermission
        && operator.department_id() != null
        && operator.department_id().equals(departmentId)) {
      return;
    }
    throw new SessionAuthenticationException(
        403, "只能审核本人负责科室或所属科室的排班申请");
  }

  private boolean isAdmin(AuthenticatedUser user) {
    return user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase);
  }

  private AuthenticatedUser requireDoctor(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.doctor_id() == null
        || user.role_codes().stream().noneMatch("DOCTOR"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return user;
  }

  private AuthenticatedUser requireReviewer(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    boolean hasReviewPermission =
        permissionAuthorizationService.hasPermission(
            user.user_id(), SCHEDULE_REQUEST_REVIEW);
    boolean canManageDepartment =
        permissionAuthorizationService.hasPermission(user.user_id(), DEPARTMENT_MANAGE);
    if (!isAdmin(user) && !canManageDepartment && !hasReviewPermission)
      throw new SessionAuthenticationException(
          403, "需要管理员、科室管理权限或排班申请审核权限");
    return user;
  }

  private ScheduleRequestResult one(long id) {
    ScheduleRequestResult result = requestMapper.selectResult(id);
    if (result == null) throw new UserRegistrationException(404, "排班申请不存在");
    return result;
  }

  private void writeLog(long userId, String type, long id, String description, String ip) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType("doctor_schedule_request");
    log.setTargetId(id);
    log.setDescription(description);
    log.setIpAddress(ip);
    operationLogMapper.insert(log);
  }
}
