package io.github.tissyboxc.harmsys.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为每次请求生成 trace_id，便于前后端和日志系统关联排错。 */
@Component
/** 为每个请求生成或透传追踪编号。 */
public class TraceIdFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = request.getHeader("X-Trace-Id");
    if (traceId == null || traceId.isBlank())
      traceId = UUID.randomUUID().toString().replace("-", "");
    MDC.put("trace_id", traceId);
    response.setHeader("X-Trace-Id", traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("trace_id");
    }
  }
}
