package io.github.tissyboxc.harmsys.security.service.impl;

import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.mapper.PermissionMapper;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
/** 根据数据库角色和权限关系判断用户是否具备指定权限。 */
public class PermissionAuthorizationServiceImpl implements PermissionAuthorizationService {

  private final PermissionMapper mapper;

  public PermissionAuthorizationServiceImpl(PermissionMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * 用户是否通过角色或直授权限获得指定权限。
   * @param userId 用户ID
   * @param permissionCode 权限编码
   */
  @Override
  public boolean hasPermission(long userId, String permissionCode) {
    return mapper.countPermission(userId, permissionCode) > 0;
  }

  /**
   * 判断已从会话读取的用户是否拥有指定权限，管理员始终放行。
   *
   * @param user 当前登录用户
   * @param permissionCode 权限编码
   */
  @Override
  public void requirePermissionForUser(AuthenticatedUser user, String permissionCode) {
    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
      return;
    }
    if (!hasPermission(user.user_id(), permissionCode)) {
      throw new SessionAuthenticationException(403, "没有访问该功能的权限");
    }
  }

  /**
   * 查看角色是否拥有这个权限
   */
  @Override
  public void requirePermission(HttpServletRequest request, String permissionCode) {
    //从Session读取角色
    AuthenticatedUser user = SessionAuth.require(request);
    requirePermissionForUser(user, permissionCode);
  }
}


