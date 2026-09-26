package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.dto.*;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.ActiveSessionService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;

/** 管理员创建账号、分配角色和维护账号状态的业务服务。 */
public interface AdminAccountService {
public AdminCreateAccountResult createPatient(AdminCreatePatientRequest request, HttpServletRequest httpRequest);

  public AdminCreateAccountResult createDoctor(AdminCreateDoctorRequest request, HttpServletRequest httpRequest);

  public AdminCreateAccountResult createRegistration(AdminCreateRegistrationRequest request, HttpServletRequest httpRequest);

  public AdminCreateAccountResult createPharmacy(AdminCreatePharmacyRequest request, HttpServletRequest httpRequest);

  public void updateStatus(long userId, int status, HttpServletRequest request);

  public void resetPassword(long userId, String password, HttpServletRequest request);

  public void setDepartmentManager(long userId, boolean enabled, Long departmentId, HttpServletRequest request);
}
