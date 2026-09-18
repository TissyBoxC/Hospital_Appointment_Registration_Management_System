package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.sessions.ActiveSessionService;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 验证用户是否一登陆并且Session是否有效
 */
public class SessionAuthenticationInterceptor implements HandlerInterceptor {
  private final ActiveSessionService activeSessions;

  public SessionAuthenticationInterceptor(ActiveSessionService activeSessions) {
    this.activeSessions = activeSessions;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    AuthenticatedUser user = SessionAuth.require(request);
    var session = request.getSession(false);
    if (session == null || !activeSessions.valid(session.getId(), user.user_id())) {
      SessionAuth.clear(request);
      throw new io.github.tissyboxc.harmsys.users.SessionAuthenticationException(
          401, "登录会话已失效，请重新登录");
    }
    activeSessions.touch(session.getId());
    return true;
  }
}
