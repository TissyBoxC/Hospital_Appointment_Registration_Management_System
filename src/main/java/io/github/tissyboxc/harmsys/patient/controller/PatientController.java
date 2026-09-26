package io.github.tissyboxc.harmsys.patient.controller;

import io.github.tissyboxc.harmsys.patient.dto.PatientProfileUpdateRequest;
import io.github.tissyboxc.harmsys.patient.service.PatientService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 患者查看和更新个人资料的接口。 */
@RestController
@RequestMapping("/api/patient/profile")
public class PatientController {
  private final PatientService service;

  public PatientController(PatientService service) {
    this.service = service;
  }

  /** 查看当前患者资料。 */
  @GetMapping
  public Map<String, Object> get(HttpServletRequest request) {
    return service.getProfile(SessionAuth.require(request));
  }

  /** 修改当前患者资料。 */
  @PutMapping
  public Map<String, Object> update(
      @Valid @RequestBody PatientProfileUpdateRequest body, HttpServletRequest request) {
    return service.updateProfile(SessionAuth.require(request), body, request.getRemoteAddr());
  }
}

