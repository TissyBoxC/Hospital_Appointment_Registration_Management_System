package io.github.tissyboxc.harmsys.security;

import io.github.tissyboxc.harmsys.security.repository.PermissionRepository;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
/** 根据数据库角色和权限关系判断用户是否具备指定权限。 */
public class PermissionAuthorizationService {

  private final PermissionRepository repository;

  public PermissionAuthorizationService(PermissionRepository repository) {
    this.repository = repository;
  }

  /**
   * 用户是否通过角色或直授权限获得指定权限。
   * @param userId 用户ID
   * @param permissionCode 权限编码
   */
  public boolean hasPermission(long userId, String permissionCode) {
    return repository.hasPermission(userId, permissionCode);
  }

  /**
   * 查看角色是否拥有这个权限
   */
  public void requirePermission(HttpServletRequest request, String permissionCode) {
    //从Session读取角色
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
      return;
    }
    if (!hasPermission(user.user_id(), permissionCode)) {
      throw new SessionAuthenticationException(403, "没有访问该功能的权限");
    }
  }
}


