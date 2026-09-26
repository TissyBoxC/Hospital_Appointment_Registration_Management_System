package io.github.tissyboxc.harmsys.admin.controller;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminStatusRequest;
import io.github.tissyboxc.harmsys.admin.service.AdminProfileService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
/** 管理员查询或修改患者、医生资料和状态的接口。 */
public class AdminProfileController {
  private final AdminProfileService service;

  public AdminProfileController(AdminProfileService service) {
    this.service = service;
  }

  @GetMapping("/patients/{id}")
  public Map<String, Object> getPatient(@PathVariable long id, HttpServletRequest request) {
    return service.patient(SessionAuth.require(request), id);
  }

  @PutMapping("/patients/{id}")
  public Map<String, Object> patient(
      @PathVariable long id,
      @Valid @RequestBody AdminPatientUpdateRequest body,
      HttpServletRequest request) {
    return service.updatePatient(
        SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @GetMapping("/doctors/{id}")
  public Map<String, Object> getDoctor(@PathVariable long id, HttpServletRequest request) {
    return service.doctor(SessionAuth.require(request), id);
  }

  @PutMapping("/doctors/{id}")
  public Map<String, Object> doctor(
      @PathVariable long id,
      @Valid @RequestBody AdminDoctorUpdateRequest body,
      HttpServletRequest request) {
    return service.updateDoctor(
        SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @PutMapping("/doctors/{id}/status")
  public void doctorStatus(
      @PathVariable long id,
      @Valid @RequestBody AdminStatusRequest body,
      HttpServletRequest request) {
    service.updateDoctorStatus(
        SessionAuth.require(request), id, body.status(), request.getRemoteAddr());
  }
}

