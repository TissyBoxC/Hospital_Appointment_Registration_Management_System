package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 基于内存窗口的基础限流；单节点部署适用，多节点应替换为 Redis 限流。 */
@Component
/** 按客户端来源限制接口请求频率。 */
public class ApiRateLimitFilter extends OncePerRequestFilter {
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Value("${harms.rate-limit.per-minute:120}")
  private int limitPerMinute;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    String context = request.getContextPath();
    String apiPrefix =
        (context == null || context.isBlank() || "/".equals(context) ? "" : context) + "/api/";
    return !path.startsWith(apiPrefix)
        || "OPTIONS".equalsIgnoreCase(request.getMethod())
        || path.endsWith("/system/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String key = key(request);
    Window window =
        windows.compute(
            key,
            (ignored, old) -> {
              long now = Instant.now().getEpochSecond();
              if (old == null || now - old.startedAt >= 60)
                return new Window(now, new AtomicInteger(1));
              old.count.incrementAndGet();
              return old;
            });
    if (window.count.get() > Math.max(1, limitPerMinute)) {
      response.setStatus(429);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
      return;
    }
    chain.doFilter(request, response);
  }

  private String key(HttpServletRequest request) {
    String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    AuthenticatedUser user = SessionAuth.current(request).orElse(null);
    return user == null ? "ip:" + ip : "user:" + user.user_id();
  }

  private record Window(long startedAt, AtomicInteger count) {}
}
