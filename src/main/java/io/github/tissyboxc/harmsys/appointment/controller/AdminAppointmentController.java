package io.github.tissyboxc.harmsys.appointment.controller;

import io.github.tissyboxc.harmsys.appointment.service.AdminAppointmentQueryService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appointments")
/** 管理员查询全量预约及分页数据。 */
public class AdminAppointmentController {
  private final AdminAppointmentQueryService service;

  public AdminAppointmentController(AdminAppointmentQueryService service) {
    this.service = service;
  }

  @GetMapping
  public List<Map<String, Object>> list(HttpServletRequest request) {
    return service.list(SessionAuth.require(request));
  }

  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    return service.page(SessionAuth.require(request), page, page_size, status);
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    return service.get(SessionAuth.require(request), id);
  }
}

