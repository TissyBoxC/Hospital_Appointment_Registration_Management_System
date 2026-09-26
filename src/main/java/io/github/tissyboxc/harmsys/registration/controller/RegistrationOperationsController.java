package io.github.tissyboxc.harmsys.registration.controller;

import io.github.tissyboxc.harmsys.registration.service.RegistrationOperationsService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 挂号员工作台：患者检索、代挂号、现场挂号、队列操作和退号。 */
@RestController
@RequestMapping("/api/registration")
public class RegistrationOperationsController {
  private final RegistrationOperationsService service;

  public RegistrationOperationsController(RegistrationOperationsService service) {
    this.service = service;
  }

  @GetMapping("/patients")
  public List<Map<String, Object>> patients(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    return service.patients(SessionAuth.require(request), keyword);
  }

  @GetMapping("/patients/{id}")
  public Map<String, Object> patient(@PathVariable long id, HttpServletRequest request) {
    return service.patient(SessionAuth.require(request), id);
  }

  @PostMapping("/appointments")
  public Map<String, Object> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.create(SessionAuth.require(request), body, request.getRemoteAddr());
  }

  @PostMapping("/appointments/{id}/refund")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refund(@PathVariable long id, HttpServletRequest request) {
    service.refund(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/queue/{id}/call-next")
  public Map<String, Object> callNext(@PathVariable long id, HttpServletRequest request) {
    return service.callNext(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/queue/{id}/mark-no-show")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void noShow(@PathVariable long id, HttpServletRequest request) {
    service.noShow(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/queue/{id}/requeue")
  public Map<String, Object> requeue(@PathVariable long id, HttpServletRequest request) {
    return service.requeue(SessionAuth.require(request), id, request.getRemoteAddr());
  }
}

