package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")

public class AdminOperationsController {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;

  public AdminOperationsController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
    this.jdbc = jdbc;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 管理员修改用户信息
   * @param id 用户ID
   * @param body 请求体
   */
  @PutMapping("/users/{id}")
  public Map<String, Object> updateUser(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String username = text(body, "username");
    Integer status = integer(body, "status");
    int changed;
    if (username != null && status != null)
      changed =
          jdbc.update(
              "UPDATE sys_user SET username=?,status=? WHERE id=? AND deleted=0",
              username.trim().toLowerCase(Locale.ROOT),
              status,
              id);
    else if (username != null)
      changed =
          jdbc.update(
              "UPDATE sys_user SET username=? WHERE id=? AND deleted=0",
              username.trim().toLowerCase(Locale.ROOT),
              id);
    else if (status != null)
      changed = jdbc.update("UPDATE sys_user SET status=? WHERE id=? AND deleted=0", status, id);
    else throw new UserRegistrationException(422, "至少提供 username 或 status");
    if (changed != 1) throw new UserRegistrationException(404, "用户不存在");
    log(operator.user_id(), "ADMIN_UPDATE_USER", "sys_user", id, "管理员修改用户资料", request);
    return jdbc.queryForMap(
        "SELECT id user_id,username,user_type,status,deleted,created_at,updated_at FROM sys_user"
            + " WHERE id=?",
        id);
  }

  /**
   * 管理员删除用户
   * @param id 用户ID
   */
  @DeleteMapping("/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.update("UPDATE sys_user SET deleted=1,status=0 WHERE id=? AND deleted=0", id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    jdbc.update("UPDATE patient SET deleted=1 WHERE user_id=?", id);
    jdbc.update("UPDATE doctor SET deleted=1,status=0 WHERE user_id=?", id);
    log(operator.user_id(), "ADMIN_DELETE_USER", "sys_user", id, "管理员逻辑删除用户", request);
  }

  @PostMapping("/users/{id}/restore")
  public Map<String, Object> restoreUser(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.update("UPDATE sys_user SET deleted=0,status=1 WHERE id=?", id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    jdbc.update("UPDATE patient SET deleted=0 WHERE user_id=?", id);
    jdbc.update("UPDATE doctor SET deleted=0,status=1 WHERE user_id=?", id);
    log(operator.user_id(), "ADMIN_RESTORE_USER", "sys_user", id, "管理员恢复用户", request);
    return jdbc.queryForMap(
        "SELECT id user_id,username,user_type,status,deleted FROM sys_user WHERE id=?", id);
  }

  /**
   * 管理员修改用户角色
   * @param userId 用户iD
   * @param body 包含角色的请求体
   */
  @PostMapping("/users/{userId}/roles")
  public Map<String, Object> assignRole(
      @PathVariable long userId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String roleCode = text(body, "role_code");
    if (roleCode == null) throw new UserRegistrationException(422, "role_code 不能为空");
    Long roleId =
        jdbc.query(
            "SELECT id FROM sys_role WHERE role_code=? AND status=1",
            rs -> rs.next() ? rs.getLong(1) : null,
            roleCode);
    if (roleId == null) throw new UserRegistrationException(404, "角色不存在");
    try {
      jdbc.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(?,?)", userId, roleId);
    } catch (org.springframework.dao.DuplicateKeyException ignored) {
    }
    log(
        operator.user_id(),
        "ADMIN_ASSIGN_ROLE",
        "sys_user",
        userId,
        "管理员分配角色 " + roleCode,
        request);
    return jdbc.queryForMap(
        "SELECT u.id user_id,u.username,r.role_code,r.role_name FROM sys_user u JOIN sys_user_role"
            + " ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE u.id=? AND r.id=?",
        userId,
        roleId);
  }

  /**
   * 管理员删除指定用户的指定角色
   * @param userId 用户ID
   * @param roleId 角色
   */
  @DeleteMapping("/users/{userId}/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeRole(
      @PathVariable long userId, @PathVariable long roleId, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.update("DELETE FROM sys_user_role WHERE user_id=? AND role_id=?", userId, roleId) != 1)
      throw new UserRegistrationException(404, "用户角色关系不存在");
    log(operator.user_id(), "ADMIN_REMOVE_ROLE", "sys_user", userId, "管理员移除角色", request);
  }

  /**
   * 角色权限修改
   * @param roleId 角色
   * @param body 权限请求体
   */
  @PostMapping("/roles/{roleId}/permissions")
  public Map<String, Object> assignPermission(
      @PathVariable long roleId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String code = text(body, "permission_code");
    if (code == null) throw new UserRegistrationException(422, "permission_code 不能为空");
    Long permissionId =
        jdbc.query(
            "SELECT id FROM sys_permission WHERE permission_code=?",
            rs -> rs.next() ? rs.getLong(1) : null,
            code);
    if (permissionId == null) throw new UserRegistrationException(404, "权限不存在");
    try {
      jdbc.update(
          "INSERT INTO sys_role_permission(role_id,permission_id) VALUES(?,?)",
          roleId,
          permissionId);
    } catch (org.springframework.dao.DuplicateKeyException ignored) {
    }
    log(
        operator.user_id(),
        "ADMIN_ASSIGN_PERMISSION",
        "sys_role",
        roleId,
        "管理员为角色分配权限 " + code,
        request);
    return jdbc.queryForMap(
        "SELECT r.id role_id,r.role_code,p.id permission_id,p.permission_code FROM sys_role r JOIN"
            + " sys_role_permission rp ON rp.role_id=r.id JOIN sys_permission p ON"
            + " p.id=rp.permission_id WHERE r.id=? AND p.id=?",
        roleId,
        permissionId);
  }

  /**
   * 管理员删除角色的角色权限
   * @param roleId 角色
   * @param permissionId 该角色拥有的权限
   */
  @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removePermission(
      @PathVariable long roleId, @PathVariable long permissionId, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.update(
            "DELETE FROM sys_role_permission WHERE role_id=? AND permission_id=?",
            roleId,
            permissionId)
        != 1) throw new UserRegistrationException(404, "角色权限关系不存在");
    log(operator.user_id(), "ADMIN_REMOVE_PERMISSION", "sys_role", roleId, "管理员移除角色权限", request);
  }

  /**
   * 管理员创建新角色
   * @param body 包含角色ID的请求体
   */
  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createRole(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String code = required(body, "role_code"), name = required(body, "role_name");
    jdbc.update(
        "INSERT INTO sys_role(role_code,role_name,status) VALUES(?,?,?)",
        code,
        name,
        integer(body, "status", 1));
    long id = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code=?", Long.class, code);
    log(operator.user_id(), "ADMIN_CREATE_ROLE", "sys_role", id, "管理员创建角色", request);
    return jdbc.queryForMap("SELECT * FROM sys_role WHERE id=?", id);
  }

  /**
   * 获取所有角色
   */
  @GetMapping("/roles")
  public List<Map<String, Object>> roles(HttpServletRequest request) {
    admin(request);
    return jdbc.queryForList("SELECT * FROM sys_role ORDER BY id");
  }

  /**
   * 获取某角色的权限
   */
  @GetMapping("/roles/{id}/permissions")
  public List<Map<String, Object>> rolePermissions(
      @PathVariable long id, HttpServletRequest request) {
    admin(request);
    return jdbc.queryForList(
        "SELECT p.* FROM sys_permission p JOIN sys_role_permission rp ON rp.permission_id=p.id"
            + " WHERE rp.role_id=? ORDER BY p.id",
        id);
  }

  /**
   * 修改角色信息
   */
  @PutMapping("/roles/{id}")
  public Map<String, Object> updateRole(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String name = required(body, "role_name");
    Integer status = integer(body, "status", 1);
    if (jdbc.update("UPDATE sys_role SET role_name=?,status=? WHERE id=?", name, status, id) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator.user_id(), "ADMIN_UPDATE_ROLE", "sys_role", id, "管理员修改角色", request);
    return jdbc.queryForMap("SELECT * FROM sys_role WHERE id=?", id);
  }

  /**
   * 删除角色
   */
  @DeleteMapping("/roles/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRole(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.update("UPDATE sys_role SET status=0 WHERE id=?", id) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator.user_id(), "ADMIN_DISABLE_ROLE", "sys_role", id, "管理员停用角色", request);
  }

  /**
   * 新增权限
   */
  @PostMapping("/permissions")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createPermission(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String code = required(body, "permission_code"), name = required(body, "permission_name");
    jdbc.update(
        "INSERT INTO sys_permission(permission_code,permission_name,type) VALUES(?,?,?)",
        code,
        name,
        integer(body, "type", 3));
    long id =
        jdbc.queryForObject(
            "SELECT id FROM sys_permission WHERE permission_code=?", Long.class, code);
    log(operator.user_id(), "ADMIN_CREATE_PERMISSION", "sys_permission", id, "管理员创建权限", request);
    return jdbc.queryForMap("SELECT * FROM sys_permission WHERE id=?", id);
  }

  /**
   * 获取权限列表
   */
  @GetMapping("/permissions")
  public List<Map<String, Object>> permissions(HttpServletRequest request) {
    admin(request);
    return jdbc.queryForList("SELECT * FROM sys_permission ORDER BY id");
  }

  /**
   * 修改某权限内容
   */
  @PutMapping("/permissions/{id}")
  public Map<String, Object> updatePermission(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    String name = required(body, "permission_name");
    Integer type = integer(body, "type", 3);
    if (jdbc.update("UPDATE sys_permission SET permission_name=?,type=? WHERE id=?", name, type, id)
        != 1) throw new UserRegistrationException(404, "权限不存在");
    log(operator.user_id(), "ADMIN_UPDATE_PERMISSION", "sys_permission", id, "管理员修改权限", request);
    return jdbc.queryForMap("SELECT * FROM sys_permission WHERE id=?", id);
  }

  /**
   * 删除某权限
   */
  @DeleteMapping("/permissions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePermission(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE id=?", Long.class, id) == 0)
      throw new UserRegistrationException(404, "权限不存在");
    jdbc.update("DELETE FROM sys_role_permission WHERE permission_id=?", id);
    jdbc.update("DELETE FROM sys_permission WHERE id=?", id);
    log(operator.user_id(), "ADMIN_DELETE_PERMISSION", "sys_permission", id, "管理员删除权限", request);
  }

  /**
   * 获取
   * 有效用户数/有效患者数/在岗医生数/启用科室数/今日预约数/今日完成就诊数/累计成功支付金额
   */
  @GetMapping("/statistics")
  public Map<String, Object> statistics(HttpServletRequest request) {
    admin(request);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "users", jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE deleted=0", Long.class));
    result.put(
        "patients",
        jdbc.queryForObject("SELECT COUNT(*) FROM patient WHERE deleted=0", Long.class));
    result.put(
        "doctors",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM doctor WHERE deleted=0 AND status=1", Long.class));
    result.put(
        "departments",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM department WHERE deleted=0 AND status=1", Long.class));
    result.put(
        "today_appointments",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointment WHERE appointment_date=CURRENT_DATE", Long.class));
    result.put(
        "today_completed_visits",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM medical_visit WHERE DATE(visit_end_at)=CURRENT_DATE",
            Long.class));
    result.put(
        "paid_amount",
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount),0) FROM payment_record WHERE status=2",
            java.math.BigDecimal.class));
    return result;
  }

  /**
   * 验证已登录且管理员身份
   */
  private AuthenticatedUser admin(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return user;
  }

  /**
   * 统一日志逻辑
   */
  private void log(
      long userId,
      String type,
      String target,
      long targetId,
      String description,
      HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        targetId,
        description,
        r.getRemoteAddr());
  }

  /**
   * Json字段转String
   * @param b json字段
   * @param k 指定字段
   * @return 指定字段中的内容，String类型
   */
  private String text(Map<String, Object> b, String k) {
    Object v = b.get(k);
    return v == null ? null : String.valueOf(v);
  }

  /**
   * 从JSON中获取字段内容
   * @param b JSON内容
   * @param k 指定字段
   */
  private String required(Map<String, Object> b, String k) {
    String v = text(b, k);
    if (v == null || v.isBlank()) throw new UserRegistrationException(422, k + " 不能为空");
    return v.trim();
  }

  /**
   * 从JSON字段中获取指定字段的INT值
   * @param b JSON内容
   * @param k 指定字段
   * @return 指定字段中的内容,INT类型
   */
  private Integer integer(Map<String, Object> b, String k) {
    Object v = b.get(k);
    return v == null ? null : Integer.valueOf(String.valueOf(v));
  }

  /**
   * 从JSON字段中获取字段内容
   * @param b JSON内容
   * @param k 指定字段
   * @param d 需要匹配的内容
   */
  private Integer integer(Map<String, Object> b, String k, int d) {
    Integer v = integer(b, k);
    return v == null ? d : v;
  }
}
