package io.github.tissyboxc.harmsys.appointment.controller;

import io.github.tissyboxc.harmsys.appointment.service.AdminAppointmentOperationsService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 管理员对预约执行操作。 */
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentOperationsController {
  private final AdminAppointmentOperationsService service;

  public AdminAppointmentOperationsController(AdminAppointmentOperationsService service) {
    this.service = service;
  }

  @PutMapping("/{id}")
  public Map<String, Object> update(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.update(SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    service.cancel(
        SessionAuth.require(request),
        id,
        body == null ? null : String.valueOf(body.get("reason")),
        request.getRemoteAddr());
  }

  @PostMapping("/{id}/expire")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void expire(@PathVariable long id, HttpServletRequest request) {
    service.expire(SessionAuth.require(request), id, request.getRemoteAddr());
  }
}

