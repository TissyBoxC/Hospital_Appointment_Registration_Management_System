package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.*;
import io.github.tissyboxc.harmsys.users.sessions.ActiveSessionService;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 管理员创建账号、分配角色和维护账号状态的业务服务。 */
public class AdminAccountService {
  private final AdminAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final ActiveSessionService activeSessionService;

  public AdminAccountService(
      AdminAccountRepository repository,
      PasswordEncoder passwordEncoder,
      ActiveSessionService activeSessionService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.activeSessionService = activeSessionService;
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminCreateAccountResult createPatient(
      AdminCreatePatientRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser operator = requireAdmin(httpRequest);
    String username = normalize(request.username());
    if (repository.usernameExists(username)) throw new UserRegistrationException(409, "用户名已存在");
    if (repository.idCardExists(request.id_card()))
      throw new UserRegistrationException(409, "身份证号已存在");
    try {
      long userId = repository.insertUser(username, passwordEncoder.encode(request.password()), 1);
      long patientId = repository.insertPatient(userId, request);
      long roleId =
          repository
              .findRoleId("PATIENT")
              .orElseThrow(() -> new IllegalStateException("系统没有初始化PATIENT角色"));
      repository.assignRole(userId, roleId);
      repository.writeLog(
          operator.user_id(),
          "ADMIN_CREATE_PATIENT",
          "patient",
          patientId,
          "管理员创建患者账号",
          httpRequest.getRemoteAddr());
      return new AdminCreateAccountResult(userId, patientId, username, "PATIENT");
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名或身份证号已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminCreateAccountResult createDoctor(
      AdminCreateDoctorRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser operator = requireAdmin(httpRequest);
    String username = normalize(request.username());
    if (repository.usernameExists(username)) throw new UserRegistrationException(409, "用户名已存在");
    if (repository.doctorNoExists(request.doctor_no()))
      throw new UserRegistrationException(409, "医生工号已存在");
    if (!repository.departmentEnabled(request.department_id()))
      throw new UserRegistrationException(422, "所属科室不存在或已停用");
    try {
      long userId = repository.insertUser(username, passwordEncoder.encode(request.password()), 2);
      long doctorId = repository.insertDoctor(userId, request);
      long roleId =
          repository
              .findRoleId("DOCTOR")
              .orElseThrow(() -> new IllegalStateException("系统没有初始化DOCTOR角色"));
      repository.assignRole(userId, roleId);
      repository.writeLog(
          operator.user_id(),
          "ADMIN_CREATE_DOCTOR",
          "doctor",
          doctorId,
          "管理员创建医生账号",
          httpRequest.getRemoteAddr());
      return new AdminCreateAccountResult(userId, doctorId, username, "DOCTOR");
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名或医生工号已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminCreateAccountResult createRegistration(
      AdminCreateRegistrationRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser operator = requireAdmin(httpRequest);
    String username = normalize(request.username());
    if (repository.usernameExists(username)) throw new UserRegistrationException(409, "用户名已存在");
    try {
      long userId = repository.insertUser(username, passwordEncoder.encode(request.password()), 4);
      long roleId =
          repository
              .findRoleId("REGISTRATION")
              .orElseThrow(() -> new IllegalStateException("系统没有初始化REGISTRATION角色"));
      repository.assignRole(userId, roleId);
      repository.writeLog(
          operator.user_id(),
          "ADMIN_CREATE_REGISTRATION",
          "sys_user",
          userId,
          "管理员创建挂号员账号",
          httpRequest.getRemoteAddr());
      return new AdminCreateAccountResult(userId, null, username, "REGISTRATION");
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminCreateAccountResult createPharmacy(
      AdminCreatePharmacyRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser operator = requireAdmin(httpRequest);
    String username = normalize(request.username());
    if (repository.usernameExists(username)) throw new UserRegistrationException(409, "用户名已存在");
    try {
      long userId = repository.insertUser(username, passwordEncoder.encode(request.password()), 5);
      long roleId =
          repository
              .findRoleId("PHARMACY")
              .orElseThrow(() -> new IllegalStateException("系统没有初始化PHARMACY角色"));
      repository.assignRole(userId, roleId);
      repository.writeLog(
          operator.user_id(),
          "ADMIN_CREATE_PHARMACY",
          "sys_user",
          userId,
          "管理员创建药房账号",
          httpRequest.getRemoteAddr());
      return new AdminCreateAccountResult(userId, null, username, "PHARMACY");
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateStatus(long userId, int status, HttpServletRequest request) {
    AuthenticatedUser operator = requireAdmin(request);
    if (status != 0 && status != 1) throw new UserRegistrationException(422, "账号状态只能是0或1");
    repository.updateStatus(userId, status);
    if (status == 0) activeSessionService.invalidateUser(userId);
    repository.writeLog(
        operator.user_id(),
        "ADMIN_UPDATE_USER_STATUS",
        "sys_user",
        userId,
        "管理员修改账号状态为" + status,
        request.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  public void resetPassword(long userId, String password, HttpServletRequest request) {
    AuthenticatedUser operator = requireAdmin(request);
    if (password == null || password.length() < 8 || password.length() > 64)
      throw new UserRegistrationException(422, "密码长度必须为8到64位");
    repository.resetPassword(userId, passwordEncoder.encode(password));
    activeSessionService.invalidateUser(userId);
    repository.writeLog(
        operator.user_id(),
        "ADMIN_RESET_PASSWORD",
        "sys_user",
        userId,
        "管理员重置账号密码",
        request.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  public void setDepartmentManager(long userId, boolean enabled, HttpServletRequest request) {
    AuthenticatedUser operator = requireAdmin(request);
    if (!repository.userExists(userId)) throw new UserRegistrationException(404, "用户不存在或已删除");
    long roleId =
        repository
            .findRoleId("DEPARTMENT_MANAGER")
            .orElseThrow(() -> new IllegalStateException("系统没有初始化DEPARTMENT_MANAGER角色"));
    if (enabled) {
      try {
        repository.assignRole(userId, roleId);
      } catch (org.springframework.dao.DuplicateKeyException ignored) {
      }
    } else {
      repository.removeRole(userId, roleId);
    }
    repository.writeLog(
        operator.user_id(),
        "ADMIN_SET_DEPARTMENT_MANAGER",
        "sys_user",
        userId,
        "管理员设置科室管理权限=" + enabled,
        request.getRemoteAddr());
  }

  private AuthenticatedUser requireAdmin(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return user;
  }

  private String normalize(String username) {
    return username.trim().toLowerCase(Locale.ROOT);
  }
}
