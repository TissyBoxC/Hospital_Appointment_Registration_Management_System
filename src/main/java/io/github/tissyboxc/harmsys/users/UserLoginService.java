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

  /**
   * 用户登录逻辑
   * @param request 包含账户密码的请求体
   */
  public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
    //获取用户名和客户端IP
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    String ip = httpRequest.getRemoteAddr() == null ? "unknown" : httpRequest.getRemoteAddr();
    //检查是否处于登录锁定状态
    //查询该用户名和IP的失败记录,存在且locker_until不为空且该值晚于当前时间,不允登录
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
    //根据用户名寻找用户
    LoginUserRecord user =
        repository
            .findLoginUser(username)
            .orElseThrow(() -> new UserLoginException(401, "用户名或密码错误"));
    //账户状态异常
    if (user.status() == null || user.status() != 1) {
      throw new UserLoginException(403, "账号已被禁用");
    }
    //该用户名对应的密码错误
    if (!passwordEncoder.matches(request.password(), user.password_hash())) {
      recordFailedLogin(username, ip);
      throw new UserLoginException(401, "用户名或密码错误");
    }
    //登陆成功后删除登陆历史
    jdbc.update("DELETE FROM login_attempt WHERE username=? AND ip_address=?", username, ip);
    //查询角色
    List<String> roleCodes = repository.findRoleCodes(user.user_id());
    String primaryRole = resolvePrimaryRole(user, roleCodes);
    //校验业务资料
    validateBusinessProfile(user, primaryRole);
    //构建用户JAVA实体
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
    //更新登陆时间
    repository.updateLastLoginTime(user.user_id());
    //建立Servlet Session
    SessionAuth.establish(httpRequest, authenticatedUser);
    //注册活跃会话
    activeSessionService.register(
        httpRequest.getSession(false).getId(),
        user.user_id(),
        SessionAuth.timeoutSeconds(httpRequest));

    return toLoginResult(authenticatedUser, httpRequest);
  }

  /**
   * 记录登陆失败,同一用户同一IP锁定15分组
   * @param username 用户名
   * @param ip 地址
   */
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

  /**
   *从Session中获取用户信息
   */
  public LoginResult currentUser(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);

    return toLoginResult(user, request);
  }

  /**
   * 用户登出
   */
  public void logout(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) activeSessionService.invalidate(session.getId());
    SessionAuth.clear(request);
  }

  /**
   * 确定主角色，根据user_type映射角色类型,如果报错则数据配置错误
   * @param user user_type
   * @param roleCodes role_codes
   */
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

  /**
   * 账户角色类型和后端角色类型不匹配或缺少
   */
  private void requireRole(List<String> roles, String requiredRole) {
    if (!roles.contains(requiredRole)) {
      throw new UserLoginException(500, "账号缺少" + requiredRole + "角色");
    }
  }

  /**
   * 校验业务资料
   * @param user 用户ID
   * @param primaryRole 主角色
   */
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

  /**
   * 将成功登陆的信息转化为JAVA实体
   * @param user 用户ID
   */
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

  /**
   * 根据用户主角色返回工作台地址
   */
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
