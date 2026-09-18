package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.AdminUserSummary;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
/** 管理员用户、角色和权限查询的数据访问层。 */
public class AdminQueryRepository {
  private final JdbcTemplate jdbc;

  public AdminQueryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   *
   * @return 返回所有用户信息
   */
  public List<AdminUserSummary> users() {
    return jdbc.query(
        "SELECT u.id"
            + " user_id,u.username,u.user_type,u.status,COALESCE(p.real_name,d.real_name,u.username)"
            + " display_name,p.id patient_id,d.id doctor_id FROM sys_user u LEFT JOIN patient p ON"
            + " p.user_id=u.id AND p.deleted=0 LEFT JOIN doctor d ON d.user_id=u.id AND d.deleted=0"
            + " WHERE u.deleted=0 ORDER BY u.id DESC",
        (rs, n) -> {
          long id = rs.getLong("user_id");
          return new AdminUserSummary(
              id,
              rs.getString("username"),
              rs.getInt("user_type"),
              rs.getInt("status"),
              rs.getString("display_name"),
              nullable(rs, "patient_id"),
              nullable(rs, "doctor_id"),
              roles(id),
              permissions(id));
        });
  }

  /**
   * 按ID查询用户
   */
  public Optional<AdminUserSummary> user(long id) {
    return users().stream().filter(x -> x.user_id().equals(id)).findFirst();
  }

  /**
   * 按ID查询用户的角色
   * @param id 用户ID
   */
  public List<String> roles(long id) {
    return jdbc.query(
        "SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE"
            + " ur.user_id=? AND r.status=1 ORDER BY r.id",
        (rs, n) -> rs.getString(1),
        id);
  }

  /**
   * 按ID查询用户的权限
   * @param id 用户ID
   */
  public List<String> permissions(long id) {
    return jdbc.query(
        "SELECT DISTINCT p.permission_code FROM sys_user_role ur JOIN sys_role_permission rp ON"
            + " rp.role_id=ur.role_id JOIN sys_permission p ON p.id=rp.permission_id JOIN sys_role"
            + " r ON r.id=ur.role_id WHERE ur.user_id=? AND r.status=1 ORDER BY p.permission_code",
        (rs, n) -> rs.getString(1),
        id);
  }

  /**
   * NULL转换
   */
  private Long nullable(java.sql.ResultSet rs, String c) throws java.sql.SQLException {
    long v = rs.getLong(c);
    return rs.wasNull() ? null : v;
  }
}
