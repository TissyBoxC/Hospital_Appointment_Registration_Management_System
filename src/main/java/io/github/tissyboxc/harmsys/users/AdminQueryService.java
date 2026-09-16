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

  private void admin(HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询");
  }

  public List<AdminUserSummary> users(HttpServletRequest r) {
    admin(r);
    return repo.users();
  }

  public AdminUserSummary user(long id, HttpServletRequest r) {
    admin(r);
    return repo.user(id).orElseThrow(() -> new UserRegistrationException(404, "用户不存在"));
  }

  public List<String> roles(long id, HttpServletRequest r) {
    admin(r);
    return repo.roles(id);
  }

  public List<String> permissions(long id, HttpServletRequest r) {
    admin(r);
    return repo.permissions(id);
  }
}
