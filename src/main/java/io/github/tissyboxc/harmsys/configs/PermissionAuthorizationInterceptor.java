package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.PermissionAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** 校验当前用户是否具备接口要求的权限编码。 */
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

  private final String permissionCode;
  private final PermissionAuthorizationService authorizationService;

  public PermissionAuthorizationInterceptor(
      PermissionAuthorizationService authorizationService, String permissionCode) {
    this.authorizationService = authorizationService;
    this.permissionCode = permissionCode;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    authorizationService.requirePermission(request, permissionCode);
    return true;
  }
}
