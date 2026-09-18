package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.PermissionAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户是否拥有某个权限
 **/
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

  private final String permissionCode;
  private final PermissionAuthorizationService authorizationService;

  public PermissionAuthorizationInterceptor(
      PermissionAuthorizationService authorizationService, String permissionCode) {
    this.authorizationService = authorizationService;
    this.permissionCode = permissionCode;
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
    authorizationService.requirePermission(request, permissionCode);
    return true;
  }
}
