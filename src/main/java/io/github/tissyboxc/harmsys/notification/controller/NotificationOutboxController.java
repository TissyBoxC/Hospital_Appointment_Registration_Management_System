package io.github.tissyboxc.harmsys.notification.controller;

import io.github.tissyboxc.harmsys.notification.service.NotificationOutboxService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 管理员查看和重试待发送通知的接口。 */
@RestController
@RequestMapping("/api/admin/notification-outbox")
public class NotificationOutboxController {
  private final NotificationOutboxService service;

  public NotificationOutboxController(NotificationOutboxService service) {
    this.service = service;
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "100") int limit,
      HttpServletRequest request) {
    return service.list(SessionAuth.require(request), status, limit);
  }

  @PostMapping("/{id}/send")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void send(@PathVariable long id, HttpServletRequest request) {
    service.send(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/{id}/retry")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void retry(@PathVariable long id, HttpServletRequest request) {
    service.retry(SessionAuth.require(request), id, request.getRemoteAddr());
  }
}

