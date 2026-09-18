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

/** 基于内存窗口的基础限流； */
@Component
/** 按客户端来源限制接口请求频率。 */
public class ApiRateLimitFilter extends OncePerRequestFilter {
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Value("${harms.rate-limit.per-minute:120}")
  private int limitPerMinute;

  /**
   * 跳过非API,OPTIONS和健康检查,不做拦截
   * @param request current HTTP request
   * @return
   */
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
    /**
     *一分钟限流120
    **/
    Window window =
        windows.compute(
            key,
            (ignored, old) -> {
              long now = Instant.now().getEpochSecond(); //当前时间距离1970 0101 0000的时间
              if (old == null || now - old.startedAt >= 60) //如果旧计数为空,即这是第一次请求,或者这次请求距上次请求小于60秒,count++,否则开新窗口重新计数
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
