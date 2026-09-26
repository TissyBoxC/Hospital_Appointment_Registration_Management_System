package io.github.tissyboxc.harmsys.system.controller;

import io.github.tissyboxc.harmsys.system.service.SystemService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 系统健康检查、通知和参数接口。 */
@RestController
@RequestMapping("/api/system")
public class SystemController {
  private final SystemService service;

  public SystemController(SystemService service) {
    this.service = service;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    return service.health();
  }

  @GetMapping("/metrics")
  public Map<String, Object> metrics(HttpServletRequest request) {
    return service.metrics(SessionAuth.require(request));
  }

  @GetMapping("/notifications")
  public List<Map<String, Object>> notifications(HttpServletRequest request) {
    return service.notifications(SessionAuth.require(request));
  }

  @PostMapping("/notifications/{id}/read")
  public void read(@PathVariable long id, HttpServletRequest request) {
    service.read(SessionAuth.require(request), id);
  }

  @GetMapping("/config")
  public List<Map<String, Object>> configs(HttpServletRequest request) {
    return service.configs(SessionAuth.require(request));
  }

  @PutMapping("/config/{key}")
  public Map<String, Object> updateConfig(
      @PathVariable String key, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.updateConfig(SessionAuth.require(request), key, body);
  }
}

