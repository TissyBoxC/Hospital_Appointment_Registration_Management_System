package io.github.tissyboxc.harmsys.security.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 用户角色与直授权限查询。 */
@Repository
public class PermissionRepository {
  private final JdbcTemplate jdbc;

  public PermissionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean hasPermission(long userId, String permissionCode) {
    Long count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_permission p
            WHERE p.permission_code = ?
              AND (
                EXISTS (
                  SELECT 1
                  FROM sys_user_role ur
                  INNER JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                  INNER JOIN sys_role r ON r.id = ur.role_id
                  WHERE ur.user_id = ?
                    AND r.status = 1
                    AND rp.permission_id = p.id
                )
                OR EXISTS (
                  SELECT 1
                  FROM sys_user_permission up
                  WHERE up.user_id = ?
                    AND up.permission_id = p.id
                )
              )
            """,
            Long.class,
            permissionCode,
            userId,
            userId);
    return count != null && count > 0;
  }
}
