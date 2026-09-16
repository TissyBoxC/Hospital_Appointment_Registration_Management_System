package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
/** 根据数据库角色和权限关系判断用户是否具备指定权限。 */
public class PermissionAuthorizationService {

  private final JdbcTemplate jdbcTemplate;

  public PermissionAuthorizationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean hasPermission(long userId, String permissionCode) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_user_role ur
            INNER JOIN sys_role_permission rp ON rp.role_id = ur.role_id
            INNER JOIN sys_permission p ON p.id = rp.permission_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = ?
              AND r.status = 1
              AND p.permission_code = ?
            """,
            Long.class,
            userId,
            permissionCode);
    return count != null && count > 0;
  }

  public void requirePermission(HttpServletRequest request, String permissionCode) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
      return;
    }
    if (!hasPermission(user.user_id(), permissionCode)) {
      throw new SessionAuthenticationException(403, "没有访问该功能的权限");
    }
  }
}
