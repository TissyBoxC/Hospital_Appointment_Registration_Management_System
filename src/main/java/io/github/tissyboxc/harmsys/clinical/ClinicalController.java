package io.github.tissyboxc.harmsys.clinical;

import io.github.tissyboxc.harmsys.clinical.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
/** 医生端诊疗、诊断和处方接口。 */
public class ClinicalController {
  private final ClinicalService s;

  public ClinicalController(ClinicalService s) {
    this.s = s;
  }

  @GetMapping("/appointments")
  public java.util.List<java.util.Map<String, Object>> appointments(HttpServletRequest r) {
    return s.doctorAppointments(r);
  }

  @GetMapping("/appointments/page")
  public java.util.Map<String, Object> appointmentsPage(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest r) {
    return s.doctorAppointmentsPage(r, page, page_size, status);
  }

  @GetMapping("/appointments/{id}")
  public java.util.Map<String, Object> appointment(@PathVariable long id, HttpServletRequest r) {
    return s.doctorAppointment(id, r);
  }

  @PutMapping("/appointments/{id}")
  public java.util.Map<String, Object> updateAppointment(
      @PathVariable long id,
      @Valid @RequestBody DoctorAppointmentUpdateRequest x,
      HttpServletRequest r) {
    return s.updateDoctorAppointment(id, x, r);
  }

  @PostMapping("/appointments/{id}/start-visit")
  public java.util.Map<String, Object> start(@PathVariable long id, HttpServletRequest r) {
    return s.startVisit(id, r);
  }

  @PostMapping("/appointments/{id}/complete")
  public java.util.Map<String, Object> complete(@PathVariable long id, HttpServletRequest r) {
    return s.completeVisit(id, r);
  }

  @GetMapping("/visits")
  public java.util.List<java.util.Map<String, Object>> visits(HttpServletRequest r) {
    return s.doctorVisits(r);
  }

  @GetMapping("/visits/{id}")
  public java.util.Map<String, Object> visit(@PathVariable long id, HttpServletRequest r) {
    return s.getVisit(id, r);
  }

  @PutMapping("/visits/{id}")
  public java.util.Map<String, Object> updateVisit(
      @PathVariable long id, @Valid @RequestBody VisitUpdateRequest x, HttpServletRequest r) {
    return s.updateVisit(id, x, r);
  }

  @GetMapping("/visits/{visitId}/diagnoses")
  public java.util.List<java.util.Map<String, Object>> diagnoses(
      @PathVariable long visitId, HttpServletRequest r) {
    return s.diagnoses(visitId, r);
  }

  @PostMapping("/visits/{visitId}/diagnoses")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> createDiagnosis(
      @PathVariable long visitId, @Valid @RequestBody DiagnosisRequest x, HttpServletRequest r) {
    return s.createDiagnosis(visitId, x, r);
  }

  @PutMapping("/diagnoses/{id}")
  public java.util.Map<String, Object> updateDiagnosis(
      @PathVariable long id, @Valid @RequestBody DiagnosisRequest x, HttpServletRequest r) {
    return s.updateDiagnosis(id, x, r);
  }

  @DeleteMapping("/diagnoses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDiagnosis(@PathVariable long id, HttpServletRequest r) {
    s.deleteDiagnosis(id, r);
  }

  @PostMapping("/prescriptions")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> createPrescription(
      @Valid @RequestBody PrescriptionRequest x, HttpServletRequest r) {
    return s.createPrescription(x, r);
  }

  @GetMapping("/prescriptions/{id}")
  public java.util.Map<String, Object> prescription(@PathVariable long id, HttpServletRequest r) {
    return s.prescription(id, r);
  }

  @PutMapping("/prescriptions/{id}/status")
  public java.util.Map<String, Object> prescriptionStatus(
      @PathVariable long id, @Valid @RequestBody Status x, HttpServletRequest r) {
    return s.updatePrescriptionStatus(id, x.status(), r);
  }

  @PostMapping("/prescriptions/{id}/items")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> addItem(
      @PathVariable long id, @Valid @RequestBody PrescriptionItemRequest x, HttpServletRequest r) {
    return s.addItem(id, x, r);
  }

  @PutMapping("/prescription-items/{id}")
  public java.util.Map<String, Object> updateItem(
      @PathVariable long id, @Valid @RequestBody PrescriptionItemRequest x, HttpServletRequest r) {
    return s.updateItem(id, x, r);
  }

  @DeleteMapping("/prescription-items/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItem(@PathVariable long id, HttpServletRequest r) {
    s.deleteItem(id, r);
  }

  /** 处方状态更新请求。 */
  public record Status(@NotNull @Min(1) @Max(3) Integer status) {}
}
