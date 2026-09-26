package io.github.tissyboxc.harmsys.doctor.service;

import io.github.tissyboxc.harmsys.doctor.dto.*;
import io.github.tissyboxc.harmsys.doctor.repository.ScheduleRequestRepository;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医生排班申请的提交、查询和审批业务。 */
@Service
public class ScheduleRequestService {
  private static final int TYPE_CREATE = 1;
  private static final int TYPE_UPDATE = 2;
  private static final int TYPE_DELETE = 3;
  private static final int STATUS_PENDING = 0;
  private static final int STATUS_APPROVED = 1;
  private static final int STATUS_REJECTED = 2;
  private static final int STATUS_CANCELLED = 3;
  private static final String SCHEDULE_REQUEST_REVIEW = "SCHEDULE_REQUEST_REVIEW";

  private final ScheduleRequestRepository repository;
  private final PermissionAuthorizationService permissionAuthorizationService;

  public ScheduleRequestService(
      ScheduleRequestRepository repository,
      PermissionAuthorizationService permissionAuthorizationService) {
    this.repository = repository;
    this.permissionAuthorizationService = permissionAuthorizationService;
  }

  /** 医生提交新增排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleRequestResult create(
      ScheduleRequestSubmit body, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    validate(body);
    assertDepartmentMatches(doctor.doctor_id(), body.department_id());
    assertNoPendingConflict(doctor.doctor_id(), null, body);
    long id =
        repository.insertRequest(
            doctor.doctor_id(),
            body.department_id(),
            null,
            TYPE_CREATE,
            body,
            doctor.user_id());
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", id, "医生提交排班申请", request.getRemoteAddr());
    return one(id);
  }

  /** 医生提交修改排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleRequestResult update(
      long scheduleId, ScheduleRequestSubmit body, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    validate(body);
    assertDepartmentMatches(doctor.doctor_id(), body.department_id());
    Map<String, Object> schedule =
        repository.findScheduleForDoctor(scheduleId, doctor.doctor_id());
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    if (((Number) schedule.get("booked_count")).intValue() > 0)
      throw new UserRegistrationException(409, "已有预约的排班不能申请修改");
    assertNoPendingConflict(
        doctor.doctor_id(),
        scheduleId,
        body);
    long id =
        repository.insertRequest(
            doctor.doctor_id(),
            body.department_id(),
            scheduleId,
            TYPE_UPDATE,
            body,
            doctor.user_id());
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", id, "医生提交排班申请", request.getRemoteAddr());
    return one(id);
  }

  /** 医生提交删除排班申请。 */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleRequestResult delete(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    Map<String, Object> schedule =
        repository.findScheduleForDoctor(scheduleId, doctor.doctor_id());
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    if (((Number) schedule.get("booked_count")).intValue() > 0)
      throw new UserRegistrationException(409, "已有预约的排班不能申请删除");
    ScheduleRequestSubmit body =
        new ScheduleRequestSubmit(
            ((Number) schedule.get("department_id")).longValue(),
            ((java.sql.Date) schedule.get("schedule_date")).toLocalDate(),
            ((Number) schedule.get("period")).intValue(),
            (java.time.LocalTime) schedule.get("start_time"),
            (java.time.LocalTime) schedule.get("end_time"),
            ((Number) schedule.get("total_count")).intValue(),
            (java.math.BigDecimal) schedule.get("fee"),
            (String) schedule.get("remark"));
    assertNoPendingConflict(doctor.doctor_id(), scheduleId, body);
    long id =
        repository.insertRequest(
            doctor.doctor_id(),
            body.department_id(),
            scheduleId,
            TYPE_DELETE,
            body,
            doctor.user_id());
    writeLog(doctor.user_id(), "SUBMIT_SCHEDULE_REQUEST", id, "医生提交排班申请", request.getRemoteAddr());
    return one(id);
  }

  /** 医生查看自己的申请。 */
  public List<ScheduleRequestResult> mine(
      Integer status, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    if (status != null) validateStatus(status);
    return repository.mine(doctor.doctor_id(), status);
  }

  /** 医生取消自己的待审核申请。 */
  @Transactional(rollbackFor = Exception.class)
  public ScheduleRequestResult cancelMine(long id, HttpServletRequest request) {
    AuthenticatedUser doctor = requireDoctor(request);
    Map<String, Object> row =
        repository.lockDoctorRequest(id, doctor.doctor_id());
    if (row == null) throw new UserRegistrationException(404, "排班申请不存在或不属于当前医生");
    if (((Number) row.get("status")).intValue() != STATUS_PENDING)
      throw new UserRegistrationException(409, "只有待审核申请可以取消");
    repository.cancelRequest(id);
    writeLog(
        doctor.user_id(),
        "CANCEL_SCHEDULE_REQUEST",
        id,
        "医生取消排班申请",
        request.getRemoteAddr());
    return one(id);
  }

  /** 查看当前账号审核范围内的排班申请。 */
  public List<ScheduleRequestResult> reviewList(
      Integer status, Long departmentId, HttpServletRequest request) {
    AuthenticatedUser operator = requireReviewer(request);
    boolean admin = isAdmin(operator);
    boolean departmentManager = isDepartmentManager(operator);
    boolean hasReviewPermission =
        permissionAuthorizationService.hasPermission(
            operator.user_id(), SCHEDULE_REQUEST_REVIEW);
    if (status != null) validateStatus(status);
    return repository.reviewList(
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
  public ScheduleRequestResult review(
      long id, boolean approved, String reviewRemark, HttpServletRequest request) {
    AuthenticatedUser operator = requireReviewer(request);
    Map<String, Object> row = repository.lockRequest(id);
    if (row == null) throw new UserRegistrationException(404, "排班申请不存在");
    if (((Number) row.get("status")).intValue() != STATUS_PENDING)
      throw new UserRegistrationException(409, "该申请已经处理，不能重复审核");
    long departmentId = ((Number) row.get("department_id")).longValue();
    assertCanReview(operator, departmentId);
    if (!approved) {
      repository.rejectRequest(id, operator.user_id(), reviewRemark);
      writeLog(
          operator.user_id(),
          "REJECT_SCHEDULE_REQUEST",
          id,
          "驳回医生排班申请",
          request.getRemoteAddr());
      return one(id);
    }

    int type = ((Number) row.get("request_type")).intValue();
    long scheduleId;
    try {
      if (type == TYPE_CREATE) scheduleId = applyCreate(row);
      else if (type == TYPE_UPDATE) {
        scheduleId = ((Number) row.get("target_schedule_id")).longValue();
        applyUpdate(scheduleId, row);
      } else if (type == TYPE_DELETE) {
        scheduleId = ((Number) row.get("target_schedule_id")).longValue();
        applyDelete(scheduleId);
      } else {
        throw new UserRegistrationException(422, "排班申请类型不合法");
      }
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已有正式排班");
    }
    repository.approveRequest(
        id, operator.user_id(), reviewRemark, type == TYPE_DELETE ? null : scheduleId);
    writeLog(
        operator.user_id(),
        "APPROVE_SCHEDULE_REQUEST",
        id,
        "通过医生排班申请",
        request.getRemoteAddr());
    return one(id);
  }

  private long applyCreate(Map<String, Object> row) {
    return insertSchedule(row);
  }

  private void applyUpdate(long scheduleId, Map<String, Object> row) {
    int updated = repository.updateSchedule(scheduleId, row);
    if (updated != 1) throw new UserRegistrationException(409, "排班已有预约，不能执行修改");
  }

  private void applyDelete(long scheduleId) {
    if (repository.deleteSchedule(scheduleId) != 1)
      throw new UserRegistrationException(409, "排班已有预约，不能执行删除");
  }

  private long insertSchedule(Map<String, Object> row) {
    return repository.insertSchedule(row);
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
    if (!repository.departmentMatchesDoctor(doctorId, departmentId))
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
  }

  /**
   * 防止同一医生的同一日期和时段存在多条待审核申请。
   */
  private void assertNoPendingConflict(
      long doctorId, Long targetScheduleId, ScheduleRequestSubmit body) {
    if (repository.countPendingConflict(
            doctorId, targetScheduleId, body.schedule_date(), body.period())
        > 0)
      throw new UserRegistrationException(409, "该日期和时段已有待审核申请");

    if (repository.countFormalScheduleConflict(
            doctorId, targetScheduleId, body.schedule_date(), body.period())
        > 0)
      throw new UserRegistrationException(
          409, targetScheduleId == null ? "该日期和时段已有正式排班" : "该日期和时段已有其他正式排班");
  }

  private void assertCanReview(AuthenticatedUser operator, long departmentId) {
    if (isAdmin(operator)) return;
    if (repository.isDepartmentManager(operator.user_id(), departmentId)) return;

    boolean hasReviewPermission =
        permissionAuthorizationService.hasPermission(
            operator.user_id(), SCHEDULE_REQUEST_REVIEW);
    if (hasReviewPermission
        && operator.department_id() != null
        && operator.department_id().longValue() == departmentId) {
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
    if (!isAdmin(user) && !isDepartmentManager(user) && !hasReviewPermission)
      throw new SessionAuthenticationException(
          403, "需要管理员、科室负责人或排班申请审核权限");
    return user;
  }

  private boolean isDepartmentManager(AuthenticatedUser user) {
    return user.role_codes().stream()
        .anyMatch("DEPARTMENT_MANAGER"::equalsIgnoreCase);
  }

  private ScheduleRequestResult one(long id) {
    return repository.findRequest(id);
  }

  private void writeLog(long userId, String type, long id, String description, String ip) {
    repository.insertOperationLog(userId, type, id, description, ip);
  }
}


