package io.github.tissyboxc.harmsys.doctor.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestResult;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestSubmit;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorScheduleRequest;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

/** 医生排班申请的提交、查询和审批业务。 */
public interface ScheduleRequestService {
public ScheduleRequestResult create(ScheduleRequestSubmit body, HttpServletRequest request);

  public ScheduleRequestResult update(long scheduleId, ScheduleRequestSubmit body, HttpServletRequest request);

  public ScheduleRequestResult delete(long scheduleId, HttpServletRequest request);

  public List<ScheduleRequestResult> mine(Integer status, HttpServletRequest request);

  public ScheduleRequestResult cancelMine(long id, HttpServletRequest request);

  public List<ScheduleRequestResult> reviewList(Integer status, Long departmentId, HttpServletRequest request);

  public ScheduleRequestResult review(long id, boolean approved, String reviewRemark, HttpServletRequest request);
}
