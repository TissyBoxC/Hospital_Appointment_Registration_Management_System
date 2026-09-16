package io.github.tissyboxc.harmsys.clinical;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
/** 患者端查询就诊和处方信息接口。 */
public class PatientClinicalController {
  private final ClinicalService s;

  public PatientClinicalController(ClinicalService s) {
    this.s = s;
  }

  @GetMapping("/visits")
  public java.util.List<java.util.Map<String, Object>> visits(HttpServletRequest r) {
    return s.patientVisits(r);
  }

  @GetMapping("/visits/{id}")
  public java.util.Map<String, Object> visit(@PathVariable long id, HttpServletRequest r) {
    return s.getVisit(id, r);
  }

  @GetMapping("/visits/{visitId}/diagnoses")
  public java.util.List<java.util.Map<String, Object>> diagnoses(
      @PathVariable long visitId, HttpServletRequest r) {
    return s.diagnoses(visitId, r);
  }

  @GetMapping("/prescriptions/{id}")
  public java.util.Map<String, Object> prescription(@PathVariable long id, HttpServletRequest r) {
    return s.prescription(id, r);
  }
}
