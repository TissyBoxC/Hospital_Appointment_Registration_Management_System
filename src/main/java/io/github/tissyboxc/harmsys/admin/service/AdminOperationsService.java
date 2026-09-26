package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 管理员用户、角色和权限业务逻辑。 */
public interface AdminOperationsService {
public Map<String, Object> updateUser(AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress);

  public void deleteUser(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> restoreUser(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> assignRole(AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress);

  public void removeRole(AuthenticatedUser operator, long userId, long roleId, String ipAddress);

  public Map<String, Object> assignRolePermission(AuthenticatedUser operator, long roleId, Map<String, Object> body, String ipAddress);

  public void removeRolePermission(AuthenticatedUser operator, long roleId, long permissionId, String ipAddress);

  public Map<String, Object> assignUserPermission(AuthenticatedUser operator, long userId, Map<String, Object> body, String ipAddress);

  public void removeUserPermission(AuthenticatedUser operator, long userId, long permissionId, String ipAddress);

  public List<Map<String, Object>> userDirectPermissions(AuthenticatedUser operator, long userId);

  public Map<String, Object> createRole(AuthenticatedUser operator, Map<String, Object> body, String ipAddress);

  public List<Map<String, Object>> roles(AuthenticatedUser operator);

  public List<Map<String, Object>> rolePermissions(AuthenticatedUser operator, long roleId);

  public Map<String, Object> updateRole(AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress);

  public void disableRole(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> createPermission(AuthenticatedUser operator, Map<String, Object> body, String ipAddress);

  public List<Map<String, Object>> permissions(AuthenticatedUser operator);

  public Map<String, Object> updatePermission(AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress);

  public void deletePermission(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> statistics(AuthenticatedUser operator);
}
