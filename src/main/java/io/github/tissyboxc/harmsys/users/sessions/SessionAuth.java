package io.github.tissyboxc.harmsys.users.sessions;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/** 读写当前 HTTP 会话中的认证用户。 */
public final class SessionAuth {

  public static final String SESSION_USER_KEY = "HARMS_AUTHENTICATED_USER";

  private SessionAuth() {}

  /**
   * 创建或获取HTTP Session,将固定属性写入Session用于验证
   * @param user 用户
   */
  public static void establish(HttpServletRequest request, AuthenticatedUser user) {
    HttpSession session = request.getSession(true);

    // 防止Session固定攻击
    request.changeSessionId();

    session.setAttribute(SESSION_USER_KEY, user);
  }

  public static Optional<AuthenticatedUser> current(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session == null) {
      return Optional.empty();
    }

    Object value = session.getAttribute(SESSION_USER_KEY);

    if (value instanceof AuthenticatedUser user) {
      return Optional.of(user);
    }

    return Optional.empty();
  }

  /**
   * 验证是否登录
   */
  public static AuthenticatedUser require(HttpServletRequest request) {
    return current(request)
            .orElseThrow(() -> new SessionAuthenticationException(401, "请先登录"));
  }

  public static int timeoutSeconds(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session == null) {
      return 0;
    }

    return session.getMaxInactiveInterval();
  }

  public static void clear(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session != null) {
      session.invalidate();
    }
  }
}
