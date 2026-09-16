package io.github.tissyboxc.harmsys.appointment;

import io.github.tissyboxc.harmsys.appointment.dto.CreateAppointmentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient/appointments")
/** 患者端预约创建、查询和取消接口。 */
public class AppointmentController {
  private final AppointmentService service;

  public AppointmentController(AppointmentService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> create(
      @Valid @RequestBody CreateAppointmentRequest r, HttpServletRequest q) {
    return service.create(r, q);
  }

  @GetMapping
  public java.util.List<java.util.Map<String, Object>> list(HttpServletRequest q) {
    return service.patientList(q);
  }

  @GetMapping("/page")
  public java.util.Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest q) {
    return service.patientPage(q, page, page_size, status);
  }

  @GetMapping("/{id}")
  public java.util.Map<String, Object> get(@PathVariable long id, HttpServletRequest q) {
    return service.patientDetail(id, q);
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(
      @PathVariable long id,
      @Valid @RequestBody(required = false) CancelRequest r,
      HttpServletRequest q) {
    service.cancelPatient(id, r == null ? null : r.reason(), q);
  }

  @PostMapping("/{id}/check-in")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void checkIn(@PathVariable long id, HttpServletRequest q) {
    var u = io.github.tissyboxc.harmsys.users.sessions.SessionAuth.require(q);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new io.github.tissyboxc.harmsys.users.SessionAuthenticationException(403, "当前账号不是患者");
    service.checkIn(id, u.patient_id(), u.user_id(), "PATIENT_CHECK_IN", q.getRemoteAddr());
  }

  /** 患者取消预约时提交的原因。 */
  public record CancelRequest(@jakarta.validation.constraints.Size(max = 255) String reason) {}
}
