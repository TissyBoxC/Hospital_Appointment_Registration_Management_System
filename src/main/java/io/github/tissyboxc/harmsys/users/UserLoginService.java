package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.LoginRequest;
import io.github.tissyboxc.harmsys.users.dto.LoginResult;
import io.github.tissyboxc.harmsys.users.dto.LoginUserRecord;
import io.github.tissyboxc.harmsys.users.sessions.ActiveSessionService;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
/** 校验账号密码、建立会话并加载用户权限。 */
public class UserLoginService {

  private final UserAccountRepository repository;

  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbc;
  private final ActiveSessionService activeSessionService;

  public UserLoginService(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder,
      JdbcTemplate jdbc,
      ActiveSessionService activeSessionService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.jdbc = jdbc;
    this.activeSessionService = activeSessionService;
  }

  public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);

    String ip = httpRequest.getRemoteAddr() == null ? "unknown" : httpRequest.getRemoteAddr();
    List<Map<String, Object>> attempts =
        jdbc.queryForList(
            "SELECT fail_count,locked_until FROM login_attempt WHERE username=? AND ip_address=?",
            username,
            ip);
    if (!attempts.isEmpty()
        && attempts.get(0).get("locked_until") != null
        && ((java.sql.Timestamp) attempts.get(0).get("locked_until"))
            .toInstant()
            .isAfter(java.time.Instant.now())) {
      throw new UserLoginException(429, "登录失败次数过多，请稍后再试");
    }

    LoginUserRecord user =
        repository
            .findLoginUser(username)
            .orElseThrow(() -> new UserLoginException(401, "用户名或密码错误"));

    if (user.status() == null || user.status() != 1) {
      throw new UserLoginException(403, "账号已被禁用");
    }

    if (!passwordEncoder.matches(request.password(), user.password_hash())) {
      recordFailedLogin(username, ip);
      throw new UserLoginException(401, "用户名或密码错误");
    }

    jdbc.update("DELETE FROM login_attempt WHERE username=? AND ip_address=?", username, ip);

    List<String> roleCodes = repository.findRoleCodes(user.user_id());

    String primaryRole = resolvePrimaryRole(user, roleCodes);

    validateBusinessProfile(user, primaryRole);

    AuthenticatedUser authenticatedUser =
        new AuthenticatedUser(
            user.user_id(),
            user.username(),
            user.display_name(),
            user.user_type(),
            primaryRole,
            roleCodes,
            user.patient_id(),
            user.doctor_id());

    repository.updateLastLoginTime(user.user_id());

    SessionAuth.establish(httpRequest, authenticatedUser);
    activeSessionService.register(
        httpRequest.getSession(false).getId(),
        user.user_id(),
        SessionAuth.timeoutSeconds(httpRequest));

    return toLoginResult(authenticatedUser, httpRequest);
  }

  private void recordFailedLogin(String username, String ip) {
    jdbc.update(
        "INSERT INTO login_attempt(username,ip_address,fail_count,locked_until,last_attempt_at)"
            + " VALUES(?,?,1,NULL,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE"
            + " fail_count=fail_count+1,locked_until=CASE WHEN fail_count+1>=5 THEN"
            + " DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE) ELSE locked_until"
            + " END,last_attempt_at=CURRENT_TIMESTAMP",
        username,
        ip);
  }

  public LoginResult currentUser(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);

    return toLoginResult(user, request);
  }

  public void logout(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) activeSessionService.invalidate(session.getId());
    SessionAuth.clear(request);
  }

  private String resolvePrimaryRole(LoginUserRecord user, List<String> roleCodes) {
    if (user.user_type() == null) {
      throw new UserLoginException(500, "账号用户类型未配置");
    }

    List<String> normalizedRoles =
        roleCodes.stream().map(role -> role.toUpperCase(Locale.ROOT)).toList();

    return switch (user.user_type()) {
      case 1 -> {
        requireRole(normalizedRoles, "PATIENT");
        yield "PATIENT";
      }

      case 2 -> {
        requireRole(normalizedRoles, "DOCTOR");
        yield "DOCTOR";
      }

      case 3 -> {
        requireRole(normalizedRoles, "ADMIN");
        yield "ADMIN";
      }

      case 4 -> {
        requireRole(normalizedRoles, "REGISTRATION");
        yield "REGISTRATION";
      }

      case 5 -> {
        requireRole(normalizedRoles, "PHARMACY");
        yield "PHARMACY";
      }

      default -> throw new UserLoginException(500, "账号用户类型不合法");
    };
  }

  private void requireRole(List<String> roles, String requiredRole) {
    if (!roles.contains(requiredRole)) {
      throw new UserLoginException(500, "账号缺少" + requiredRole + "角色");
    }
  }

  private void validateBusinessProfile(LoginUserRecord user, String primaryRole) {
    if ("PATIENT".equals(primaryRole) && user.patient_id() == null) {
      throw new UserLoginException(500, "患者账号缺少患者资料");
    }

    if ("DOCTOR".equals(primaryRole)) {
      if (user.doctor_id() == null) {
        throw new UserLoginException(500, "医生账号缺少医生资料");
      }

      if (user.doctor_status() == null || user.doctor_status() != 1) {
        throw new UserLoginException(403, "医生当前不可执业");
      }
    }
  }

  private LoginResult toLoginResult(AuthenticatedUser user, HttpServletRequest request) {
    return new LoginResult(
        user.user_id(),
        user.username(),
        user.display_name(),
        user.user_type(),
        user.primary_role(),
        user.role_codes(),
        user.patient_id(),
        user.doctor_id(),
        resolveRedirectPath(user.primary_role()),
        SessionAuth.timeoutSeconds(request),
        LocalDateTime.now());
  }

  private String resolveRedirectPath(String primaryRole) {
    return switch (primaryRole) {
      case "PATIENT" -> "/patient/home";
      case "DOCTOR" -> "/doctor/home";
      case "ADMIN" -> "/admin/home";
      case "REGISTRATION" -> "/registration/home";
      case "PHARMACY" -> "/pharmacy/home";
      default -> "/home";
    };
  }
}
