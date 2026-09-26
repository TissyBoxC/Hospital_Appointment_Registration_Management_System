package io.github.tissyboxc.harmsys.admin.service.impl;

import io.github.tissyboxc.harmsys.admin.service.AdminOperationsService;

import io.github.tissyboxc.harmsys.admin.mapper.AdminAccessMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
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
public class AdminOperationsServiceImpl implements AdminOperationsService {
  private final AdminAccessMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public AdminOperationsServiceImpl(
      AdminAccessMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateUser(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String username = text(body, "username");
    Integer status = integer(body, "status");
    if (username == null && status == null)
      throw new UserRegistrationException(422, "至少提供 username 或 status");
    String normalized = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    if (mapper.updateUser(id, normalized, status) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    log(operator, "ADMIN_UPDATE_USER", "sys_user", id, "管理员修改用户资料", ipAddress);
    return mapper.selectUser(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteUser(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (mapper.deleteUser(id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    mapper.deletePatientByUser(id);
    mapper.deleteDoctorByUser(id);
    log(operator, "ADMIN_DELETE_USER", "sys_user", id, "管理员逻辑删除用户", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> restoreUser(
      AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (mapper.restoreUser(id) != 1)
      throw new UserRegistrationException(404, "用户不存在");
    mapper.restorePatientByUser(id);
    mapper.restoreDoctorByUser(id);
    log(operator, "ADMIN_RESTORE_USER", "sys_user", id, "管理员恢复用户", ipAddress);
    return mapper.selectRestoredUser(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> assignRole(
      AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String roleCode = required(body, "role_code");
    Long roleId = mapper.selectRoleId(roleCode);
    if (roleId == null) throw new UserRegistrationException(404, "角色不存在");
    try {
      mapper.insertUserRole(userId, roleId);
    } catch (DuplicateKeyException ignored) {
    }
    log(operator, "ADMIN_ASSIGN_ROLE", "sys_user", userId, "管理员分配角色 " + roleCode, ipAddress);
    return mapper.selectUserRole(userId, roleId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void removeRole(
      AuthenticatedUser operator, long userId, long roleId, String ipAddress) {
    requireAdmin(operator);
    if (mapper.deleteUserRole(userId, roleId) != 1)
      throw new UserRegistrationException(404, "用户角色关系不存在");
    log(operator, "ADMIN_REMOVE_ROLE", "sys_user", userId, "管理员移除角色", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> assignRolePermission(
      AuthenticatedUser operator, long roleId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String code = required(body, "permission_code");
    Long permissionId = mapper.selectPermissionId(code);
    if (permissionId == null) throw new UserRegistrationException(404, "权限不存在");
    try {
      mapper.insertRolePermission(roleId, permissionId);
    } catch (DuplicateKeyException ignored) {
    }
    log(
        operator,
        "ADMIN_ASSIGN_PERMISSION",
        "sys_role",
        roleId,
        "管理员为角色分配权限 " + code,
        ipAddress);
    return mapper.selectRolePermission(roleId, permissionId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void removeRolePermission(
      AuthenticatedUser operator, long roleId, long permissionId, String ipAddress) {
    requireAdmin(operator);
    if (mapper.deleteRolePermission(roleId, permissionId) != 1)
      throw new UserRegistrationException(404, "角色权限关系不存在");
    log(operator, "ADMIN_REMOVE_PERMISSION", "sys_role", roleId, "管理员移除角色权限", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> assignUserPermission(
      AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (mapper.countActiveUser(userId) == 0)
      throw new UserRegistrationException(404, "用户不存在");
    String code = required(body, "permission_code");
    Long permissionId = mapper.selectPermissionId(code);
    if (permissionId == null) throw new UserRegistrationException(404, "权限不存在");
    try {
      mapper.insertUserPermission(userId, permissionId, operator.user_id());
    } catch (DuplicateKeyException ignored) {
    }
    log(
        operator,
        "ADMIN_ASSIGN_USER_PERMISSION",
        "sys_user",
        userId,
        "管理员直接授予用户权限 " + code,
        ipAddress);
    return mapper.selectUserDirectPermission(userId, permissionId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void removeUserPermission(
      AuthenticatedUser operator, long userId, long permissionId, String ipAddress) {
    requireAdmin(operator);
    if (mapper.deleteUserPermission(userId, permissionId) != 1)
      throw new UserRegistrationException(404, "用户直授权限不存在");
    log(operator, "ADMIN_REMOVE_USER_PERMISSION", "sys_user", userId, "管理员移除用户直授权限", ipAddress);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> userDirectPermissions(
      AuthenticatedUser operator, long userId) {
    requireAdmin(operator);
    return mapper.selectUserDirectPermissions(userId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> createRole(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String code = required(body, "role_code");
    mapper.insertRole(code, required(body, "role_name"), integer(body, "status", 1));
    Long roleId = mapper.selectRoleIdByCode(code);
    if (roleId == null) throw new IllegalStateException("创建角色失败");
    long id = roleId;
    log(operator, "ADMIN_CREATE_ROLE", "sys_role", id, "管理员创建角色", ipAddress);
    return mapper.selectRole(id);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> roles(AuthenticatedUser operator) {
    requireAdmin(operator);
    return mapper.selectRoles();
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> rolePermissions(AuthenticatedUser operator, long roleId) {
    requireAdmin(operator);
    return mapper.selectRolePermissions(roleId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateRole(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (mapper.updateRole(id, required(body, "role_name"), integer(body, "status", 1)) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator, "ADMIN_UPDATE_ROLE", "sys_role", id, "管理员修改角色", ipAddress);
    return mapper.selectRole(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void disableRole(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (mapper.disableRole(id) != 1)
      throw new UserRegistrationException(404, "角色不存在");
    log(operator, "ADMIN_DISABLE_ROLE", "sys_role", id, "管理员停用角色", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> createPermission(
      AuthenticatedUser operator, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    String code = required(body, "permission_code");
    mapper.insertPermission(
        code, required(body, "permission_name"), integer(body, "type", 3));
    Long permissionId = mapper.selectPermissionIdByCode(code);
    if (permissionId == null) throw new IllegalStateException("创建权限失败");
    long id = permissionId;
    log(operator, "ADMIN_CREATE_PERMISSION", "sys_permission", id, "管理员创建权限", ipAddress);
    return mapper.selectPermission(id);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> permissions(AuthenticatedUser operator) {
    requireAdmin(operator);
    return mapper.selectPermissions();
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updatePermission(
      AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress) {
    requireAdmin(operator);
    if (mapper.updatePermission(
            id, required(body, "permission_name"), integer(body, "type", 3))
        != 1) throw new UserRegistrationException(404, "权限不存在");
    log(operator, "ADMIN_UPDATE_PERMISSION", "sys_permission", id, "管理员修改权限", ipAddress);
    return mapper.selectPermission(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deletePermission(AuthenticatedUser operator, long id, String ipAddress) {
    requireAdmin(operator);
    if (mapper.countPermission(id) == 0)
      throw new UserRegistrationException(404, "权限不存在");
    mapper.deletePermissionRelations(id);
    mapper.deletePermission(id);
    log(operator, "ADMIN_DELETE_PERMISSION", "sys_permission", id, "管理员删除权限", ipAddress);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> statistics(AuthenticatedUser operator) {
    requireAdmin(operator);
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("users", mapper.countUsers());
    result.put("patients", mapper.countPatients());
    result.put("doctors", mapper.countDoctors());
    result.put("departments", mapper.countDepartments());
    result.put("today_appointments", mapper.countTodayAppointments());
    result.put("today_completed_visits", mapper.countTodayCompletedVisits());
    result.put("paid_amount", mapper.selectPaidAmount());
    return result;
  }

  private void log(
      AuthenticatedUser operator,
      String type,
      String target,
      long targetId,
      String description,
      String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(operator.user_id());
    log.setOperationType(type);
    log.setTargetType(target);
    log.setTargetId(targetId);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
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

