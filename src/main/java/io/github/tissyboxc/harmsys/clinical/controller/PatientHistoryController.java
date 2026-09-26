package io.github.tissyboxc.harmsys.clinical.controller;

import io.github.tissyboxc.harmsys.clinical.service.PatientHistoryService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 患者端完整医疗历史查询。 */
@RestController
@RequestMapping("/api/patient")
public class PatientHistoryController {
  private final PatientHistoryService service;

  public PatientHistoryController(PatientHistoryService service) {
    this.service = service;
  }

  @GetMapping("/prescriptions")
  public List<Map<String, Object>> prescriptions(HttpServletRequest request) {
    return service.prescriptions(SessionAuth.require(request));
  }

  @GetMapping("/prescriptions/{id}/items")
  public List<Map<String, Object>> items(
      @PathVariable long id, HttpServletRequest request) {
    return service.items(SessionAuth.require(request), id);
  }

  @GetMapping("/diagnoses")
  public List<Map<String, Object>> diagnoses(
      @RequestParam(required = false) Long visit_id, HttpServletRequest request) {
    return service.diagnoses(SessionAuth.require(request), visit_id);
  }
}

