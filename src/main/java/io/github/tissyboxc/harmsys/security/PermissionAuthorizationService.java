package io.github.tissyboxc.harmsys.security;

import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;

/** 根据数据库角色和权限关系判断用户是否具备指定权限。 */
public interface PermissionAuthorizationService {
public boolean hasPermission(long userId, String permissionCode);

  public void requirePermissionForUser(AuthenticatedUser user, String permissionCode);

  public void requirePermission(HttpServletRequest request, String permissionCode);
}
