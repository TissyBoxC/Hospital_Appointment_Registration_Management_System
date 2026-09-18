package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.AdminUserSummary;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
/** 管理员查询用户角色和权限信息的业务服务。 */
public class AdminQueryService {
  private final AdminQueryRepository repo;

  public AdminQueryService(AdminQueryRepository repo) {
    this.repo = repo;
  }

  /**
   * 验证已登录且管理员
   */
  private void admin(HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询");
  }

  /**
   * 查询所有用户
   */
  public List<AdminUserSummary> users(HttpServletRequest r) {
    admin(r);
    return repo.users();
  }

  /**
   * 按ID查询用户
   * @param id 用户ID
   */
  public AdminUserSummary user(long id, HttpServletRequest r) {
    admin(r);
    return repo.user(id).orElseThrow(() -> new UserRegistrationException(404, "用户不存在"));
  }

  /**
   * 按ID查询用户角色
   */
  public List<String> roles(long id, HttpServletRequest r) {
    admin(r);
    return repo.roles(id);
  }

  /**
   * 按ID查询用户权限
   */
  public List<String> permissions(long id, HttpServletRequest r) {
    admin(r);
    return repo.permissions(id);
  }
}
