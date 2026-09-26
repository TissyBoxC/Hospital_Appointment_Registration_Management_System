package io.github.tissyboxc.harmsys.admin.repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 管理员用户、角色、权限与统计数据访问。 */
@Repository
public class AdminAccessRepository {
  private final JdbcTemplate jdbc;

  public AdminAccessRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int updateUser(long id, String username, Integer status) {
    if (username != null && status != null)
      return jdbc.update(
          "UPDATE sys_user SET username=?,status=? WHERE id=? AND deleted=0",
          username,
          status,
          id);
    if (username != null)
      return jdbc.update("UPDATE sys_user SET username=? WHERE id=? AND deleted=0", username, id);
    return jdbc.update("UPDATE sys_user SET status=? WHERE id=? AND deleted=0", status, id);
  }

  public Map<String, Object> findUser(long id) {
    return one(
        "SELECT id user_id,username,user_type,status,deleted,created_at,updated_at FROM sys_user"
            + " WHERE id=?",
        id);
  }

  public int deleteUser(long id) {
    int changed =
        jdbc.update("UPDATE sys_user SET deleted=1,status=0 WHERE id=? AND deleted=0", id);
    if (changed == 1) {
      jdbc.update("UPDATE patient SET deleted=1 WHERE user_id=?", id);
      jdbc.update("UPDATE doctor SET deleted=1,status=0 WHERE user_id=?", id);
    }
    return changed;
  }

  public int restoreUser(long id) {
    int changed = jdbc.update("UPDATE sys_user SET deleted=0,status=1 WHERE id=?", id);
    if (changed == 1) {
      jdbc.update("UPDATE patient SET deleted=0 WHERE user_id=?", id);
      jdbc.update("UPDATE doctor SET deleted=0,status=1 WHERE user_id=?", id);
    }
    return changed;
  }

  public Map<String, Object> findRestoredUser(long id) {
    return one("SELECT id user_id,username,user_type,status,deleted FROM sys_user WHERE id=?", id);
  }

  public Long findRoleId(String roleCode) {
    return jdbc.query(
        "SELECT id FROM sys_role WHERE role_code=? AND status=1",
        rs -> rs.next() ? rs.getLong(1) : null,
        roleCode);
  }

  public void assignRole(long userId, long roleId) {
    jdbc.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(?,?)", userId, roleId);
  }

  public Map<String, Object> findUserRole(long userId, long roleId) {
    return one(
        "SELECT u.id user_id,u.username,r.role_code,r.role_name FROM sys_user u JOIN sys_user_role"
            + " ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE u.id=? AND r.id=?",
        userId,
        roleId);
  }

  public int removeRole(long userId, long roleId) {
    return jdbc.update(
        "DELETE FROM sys_user_role WHERE user_id=? AND role_id=?", userId, roleId);
  }

  public Long findPermissionId(String permissionCode) {
    return jdbc.query(
        "SELECT id FROM sys_permission WHERE permission_code=?",
        rs -> rs.next() ? rs.getLong(1) : null,
        permissionCode);
  }

  public void assignRolePermission(long roleId, long permissionId) {
    jdbc.update(
        "INSERT INTO sys_role_permission(role_id,permission_id) VALUES(?,?)",
        roleId,
        permissionId);
  }

  public Map<String, Object> findRolePermission(long roleId, long permissionId) {
    return one(
        "SELECT r.id role_id,r.role_code,p.id permission_id,p.permission_code FROM sys_role r JOIN"
            + " sys_role_permission rp ON rp.role_id=r.id JOIN sys_permission p ON"
            + " p.id=rp.permission_id WHERE r.id=? AND p.id=?",
        roleId,
        permissionId);
  }

  public int removeRolePermission(long roleId, long permissionId) {
    return jdbc.update(
        "DELETE FROM sys_role_permission WHERE role_id=? AND permission_id=?",
        roleId,
        permissionId);
  }

  public boolean userExists(long userId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE id=? AND deleted=0", Long.class, userId)
        > 0;
  }

  public void assignUserPermission(long userId, long permissionId, long grantedBy) {
    jdbc.update(
        "INSERT INTO sys_user_permission(user_id,permission_id,granted_by_user_id) VALUES(?,?,?)",
        userId,
        permissionId,
        grantedBy);
  }

  public Map<String, Object> findUserDirectPermission(long userId, long permissionId) {
    return one(
        "SELECT u.id user_id,u.username,p.id permission_id,p.permission_code,p.permission_name"
            + " FROM sys_user u JOIN sys_user_permission up ON up.user_id=u.id JOIN sys_permission p"
            + " ON p.id=up.permission_id WHERE u.id=? AND p.id=?",
        userId,
        permissionId);
  }

  public int removeUserPermission(long userId, long permissionId) {
    return jdbc.update(
        "DELETE FROM sys_user_permission WHERE user_id=? AND permission_id=?",
        userId,
        permissionId);
  }

  public List<Map<String, Object>> userDirectPermissions(long userId) {
    return jdbc.queryForList(
        "SELECT p.id permission_id,p.permission_code,p.permission_name,p.type,up.created_at"
            + " FROM sys_user_permission up JOIN sys_permission p ON p.id=up.permission_id"
            + " WHERE up.user_id=? ORDER BY p.permission_code",
        userId);
  }

  public long createRole(String code, String name, int status) {
    jdbc.update(
        "INSERT INTO sys_role(role_code,role_name,status) VALUES(?,?,?)", code, name, status);
    return jdbc.queryForObject(
        "SELECT id FROM sys_role WHERE role_code=?", Long.class, code);
  }

  public List<Map<String, Object>> roles() {
    return jdbc.queryForList("SELECT * FROM sys_role ORDER BY id");
  }

  public Map<String, Object> role(long id) {
    return one("SELECT * FROM sys_role WHERE id=?", id);
  }

  public List<Map<String, Object>> rolePermissions(long roleId) {
    return jdbc.queryForList(
        "SELECT p.* FROM sys_permission p JOIN sys_role_permission rp ON rp.permission_id=p.id"
            + " WHERE rp.role_id=? ORDER BY p.id",
        roleId);
  }

  public int updateRole(long id, String name, int status) {
    return jdbc.update(
        "UPDATE sys_role SET role_name=?,status=? WHERE id=?", name, status, id);
  }

  public int disableRole(long id) {
    return jdbc.update("UPDATE sys_role SET status=0 WHERE id=?", id);
  }

  public long createPermission(String code, String name, int type) {
    jdbc.update(
        "INSERT INTO sys_permission(permission_code,permission_name,type) VALUES(?,?,?)",
        code,
        name,
        type);
    return jdbc.queryForObject(
        "SELECT id FROM sys_permission WHERE permission_code=?", Long.class, code);
  }

  public List<Map<String, Object>> permissions() {
    return jdbc.queryForList("SELECT * FROM sys_permission ORDER BY id");
  }

  public Map<String, Object> permission(long id) {
    return one("SELECT * FROM sys_permission WHERE id=?", id);
  }

  public int updatePermission(long id, String name, int type) {
    return jdbc.update(
        "UPDATE sys_permission SET permission_name=?,type=? WHERE id=?", name, type, id);
  }

  public boolean permissionExists(long id) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_permission WHERE id=?", Long.class, id)
        > 0;
  }

  public void deletePermission(long id) {
    jdbc.update("DELETE FROM sys_role_permission WHERE permission_id=?", id);
    jdbc.update("DELETE FROM sys_permission WHERE id=?", id);
  }

  public Map<String, Object> statistics() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("users", count("SELECT COUNT(*) FROM sys_user WHERE deleted=0"));
    result.put("patients", count("SELECT COUNT(*) FROM patient WHERE deleted=0"));
    result.put("doctors", count("SELECT COUNT(*) FROM doctor WHERE deleted=0 AND status=1"));
    result.put(
        "departments", count("SELECT COUNT(*) FROM department WHERE deleted=0 AND status=1"));
    result.put(
        "today_appointments",
        count("SELECT COUNT(*) FROM appointment WHERE appointment_date=CURRENT_DATE"));
    result.put(
        "today_completed_visits",
        count("SELECT COUNT(*) FROM medical_visit WHERE DATE(visit_end_at)=CURRENT_DATE"));
    result.put(
        "paid_amount",
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount),0) FROM payment_record WHERE status=2",
            BigDecimal.class));
    return result;
  }

  public void insertOperationLog(
      long userId,
      String type,
      String target,
      long targetId,
      String description,
      String ipAddress) {
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,target_id,description,"
            + "ip_address) VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        targetId,
        description,
        ipAddress);
  }

  private long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
