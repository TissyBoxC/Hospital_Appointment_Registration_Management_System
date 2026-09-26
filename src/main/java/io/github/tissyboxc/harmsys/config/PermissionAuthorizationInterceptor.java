package io.github.tissyboxc.harmsys.config;

import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户是否拥有某个权限
 **/
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

  private final String[] permissionCodes;
  private final PermissionAuthorizationService authorizationService;

  public PermissionAuthorizationInterceptor(
      PermissionAuthorizationService authorizationService, String... permissionCodes) {
    this.authorizationService = authorizationService;
    this.permissionCodes = permissionCodes;
  }

  /**
   * 在Conrtoller方法执行前调用,
   * @param request current HTTP request
   * @param response current HTTP response
   * @param handler chosen handler to execute, for type and/or instance evaluation
   * @return
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) return true;
    for (String permissionCode : permissionCodes) {
      if (authorizationService.hasPermission(user.user_id(), permissionCode)) return true;
    }
    throw new SessionAuthenticationException(403, "没有访问该功能的权限");
  }
}


