package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.repository.AdminAccessRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员用户、角色和权限业务逻辑。 */
@Service
public class AdminOperationsService {
  private final AdminAccessRepository repository;

  public AdminOperationsService(AdminAccessRepository repository) {
    this.repository = repository;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateUser(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String username = text(body, "username");
    Integer status = integer(body, "status");
    if (username == null && status == null)
      throw new UserRegistrationException(422, "至少提供 username 或 status");
    String normalized = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    if (repository.updateUser(id, normalized, status) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    log(operator, "ADMIN_UPDATE_USER", "sys_user", id, "管理员修改用户资料", ipAddress);
    return repository.findUser(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteUser(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (repository.deleteUser(id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    log(operator, "ADMIN_DELETE_USER", "sys_user", id, "管理员逻辑删除用户", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> restoreUser(
      AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (repository.restoreUser(id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    log(operator, "ADMIN_RESTORE_USER", "sys_user", id, "管理员恢复用户", ipAddress);
    return repository.findRestoredUser(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> assignRole(
      AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String roleCode = required(body, "role_code");
    Long roleId = repository.findRoleId(roleCode);
    if (roleId == null) throw new UserRegistrationException(404, "角色不存在");
    try {
      repository.assignRole(userId, roleId);
    } catch (DuplicateKeyException ignored) {
    }
    log(operator, "ADMIN_ASSIGN_ROLE", "sys_user", userId, "管理员分配角色 " + roleCode, ipAddress);
    return repository.findUserRole(userId, roleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public void removeRole(
      AuthenticatedUser operator, long userId, long roleId, String ipAddress) {
    requireAdmin(operator);
    if (repository.removeRole(userId, roleId) != 1)
      throw new UserRegistrationException(404, "用户角色关系不存在");
    log(operator, "ADMIN_REMOVE_ROLE", "sys_user", userId, "管理员移除角色", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> assignRolePermission(
      AuthenticatedUser operator, long roleId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String code = required(body, "permission_code");
    Long permissionId = repository.findPermissionId(code);
    if (permissionId == null) throw new UserRegistrationException(404, "权限不存在");
    try {
      repository.assignRolePermission(roleId, permissionId);
    } catch (DuplicateKeyException ignored) {
    }
    log(
        operator,
        "ADMIN_ASSIGN_PERMISSION",
        "sys_role",
        roleId,
        "管理员为角色分配权限 " + code,
        ipAddress);
    return repository.findRolePermission(roleId, permissionId);
  }

  @Transactional(rollbackFor = Exception.class)
  public void removeRolePermission(
      AuthenticatedUser operator, long roleId, long permissionId, String ipAddress) {
    requireAdmin(operator);
    if (repository.removeRolePermission(roleId, permissionId) != 1)
      throw new UserRegistrationException(404, "角色权限关系不存在");
    log(operator, "ADMIN_REMOVE_PERMISSION", "sys_role", roleId, "管理员移除角色权限", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> assignUserPermission(
      AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (!repository.userExists(userId))
      throw new UserRegistrationException(404, "用户不存在");
    String code = required(body, "permission_code");
    Long permissionId = repository.findPermissionId(code);
    if (permissionId == null) throw new UserRegistrationException(404, "权限不存在");
    try {
      repository.assignUserPermission(userId, permissionId, operator.user_id());
    } catch (DuplicateKeyException ignored) {
    }
    log(
        operator,
        "ADMIN_ASSIGN_USER_PERMISSION",
        "sys_user",
        userId,
        "管理员直接授予用户权限 " + code,
        ipAddress);
    return repository.findUserDirectPermission(userId, permissionId);
  }

  @Transactional(rollbackFor = Exception.class)
  public void removeUserPermission(
      AuthenticatedUser operator, long userId, long permissionId, String ipAddress) {
    requireAdmin(operator);
    if (repository.removeUserPermission(userId, permissionId) != 1)
      throw new UserRegistrationException(404, "用户直授权限不存在");
    log(operator, "ADMIN_REMOVE_USER_PERMISSION", "sys_user", userId, "管理员移除用户直授权限", ipAddress);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> userDirectPermissions(
      AuthenticatedUser operator, long userId) {
    requireAdmin(operator);
    return repository.userDirectPermissions(userId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> createRole(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    long id =
        repository.createRole(
            required(body, "role_code"), required(body, "role_name"), integer(body, "status", 1));
    log(operator, "ADMIN_CREATE_ROLE", "sys_role", id, "管理员创建角色", ipAddress);
    return repository.role(id);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> roles(AuthenticatedUser operator) {
    requireAdmin(operator);
    return repository.roles();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> rolePermissions(AuthenticatedUser operator, long roleId) {
    requireAdmin(operator);
    return repository.rolePermissions(roleId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateRole(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (repository.updateRole(id, required(body, "role_name"), integer(body, "status", 1)) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator, "ADMIN_UPDATE_ROLE", "sys_role", id, "管理员修改角色", ipAddress);
    return repository.role(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void disableRole(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (repository.disableRole(id) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator, "ADMIN_DISABLE_ROLE", "sys_role", id, "管理员停用角色", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> createPermission(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    long id =
        repository.createPermission(
            required(body, "permission_code"),
            required(body, "permission_name"),
            integer(body, "type", 3));
    log(operator, "ADMIN_CREATE_PERMISSION", "sys_permission", id, "管理员创建权限", ipAddress);
    return repository.permission(id);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> permissions(AuthenticatedUser operator) {
    requireAdmin(operator);
    return repository.permissions();
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updatePermission(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (repository.updatePermission(
            id, required(body, "permission_name"), integer(body, "type", 3))
        != 1) throw new UserRegistrationException(404, "权限不存在");
    log(operator, "ADMIN_UPDATE_PERMISSION", "sys_permission", id, "管理员修改权限", ipAddress);
    return repository.permission(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deletePermission(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (!repository.permissionExists(id))
      throw new UserRegistrationException(404, "权限不存在");
    repository.deletePermission(id);
    log(operator, "ADMIN_DELETE_PERMISSION", "sys_permission", id, "管理员删除权限", ipAddress);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> statistics(AuthenticatedUser operator) {
    requireAdmin(operator);
    return repository.statistics();
  }

  private void log(
      AuthenticatedUser operator,
      String type,
      String target,
      long targetId,
      String description,
      String ipAddress) {
    repository.insertOperationLog(
        operator.user_id(), type, target, targetId, description, ipAddress);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
  }

  private String text(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private String required(Map<String, Object> body, String key) {
    String value = text(body, key);
    if (value == null || value.isBlank())
      throw new UserRegistrationException(422, key + " 不能为空");
    return value.trim();
  }

  private Integer integer(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? null : Integer.valueOf(String.valueOf(value));
  }

  private int integer(Map<String, Object> body, String key, int defaultValue) {
    Integer value = integer(body, key);
    return value == null ? defaultValue : value;
  }
}

