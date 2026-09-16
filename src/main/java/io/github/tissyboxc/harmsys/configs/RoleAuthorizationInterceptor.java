package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** 校验当前用户是否具备接口要求的角色。 */
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

  private final String requiredRole;

  public RoleAuthorizationInterceptor(String requiredRole) {
    this.requiredRole = requiredRole;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    AuthenticatedUser user = SessionAuth.require(request);

    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase)) {
      return true;
    }

    boolean allowed =
        user.role_codes().stream().anyMatch(role -> requiredRole.equalsIgnoreCase(role));

    if (!allowed) {
      throw new SessionAuthenticationException(403, "没有访问该功能的权限");
    }

    return true;
  }
}
