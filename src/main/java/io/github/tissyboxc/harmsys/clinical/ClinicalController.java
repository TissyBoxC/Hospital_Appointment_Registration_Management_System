package io.github.tissyboxc.harmsys.clinical;

import io.github.tissyboxc.harmsys.clinical.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 医生端诊疗、诊断和处方接口。
 */
@RestController
@RequestMapping("/api/doctor")
public class ClinicalController {
  private final ClinicalService service;

  public ClinicalController(ClinicalService service) {
    this.service = service;
  }

  /**
   * 医生端查询自己的患者信息
   */
  @GetMapping("/appointments")
  public java.util.List<java.util.Map<String, Object>> appointments(HttpServletRequest request) {
    return service.doctorAppointments(request);
  }

  /**
   * 医生端分页查询患者信息
   */
  @GetMapping("/appointments/page")
  public java.util.Map<String, Object> appointmentsPage(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    return service.doctorAppointmentsPage(request, page, page_size, status);
  }

  /**
   * 按ID查询预约信息
   */
  @GetMapping("/appointments/{id}")
  public java.util.Map<String, Object> appointment(@PathVariable long id, HttpServletRequest request) {
    return service.doctorAppointment(id, request);
  }

  /**
   * 医生修改患者预约信息
   */
  @PutMapping("/appointments/{id}")
  public java.util.Map<String, Object> updateAppointment(
      @PathVariable long id,
      @Valid @RequestBody DoctorAppointmentUpdateRequest x,
      HttpServletRequest request) {
    return service.updateDoctorAppointment(id, x, request);
  }

  @PostMapping("/appointments/{id}/start-visit")
  public java.util.Map<String, Object> start(@PathVariable long id, HttpServletRequest request) {
    return service.startVisit(id, request);
  }

  @PostMapping("/appointments/{id}/complete")
  public java.util.Map<String, Object> complete(@PathVariable long id, HttpServletRequest request) {
    return service.completeVisit(id, request);
  }

  @GetMapping("/visits")
  public java.util.List<java.util.Map<String, Object>> visits(HttpServletRequest request) {
    return service.doctorVisits(request);
  }

  @GetMapping("/visits/{id}")
  public java.util.Map<String, Object> visit(@PathVariable long id, HttpServletRequest request) {
    return service.getVisit(id, request);
  }

  @PutMapping("/visits/{id}")
  public java.util.Map<String, Object> updateVisit(
      @PathVariable long id, @Valid @RequestBody VisitUpdateRequest x, HttpServletRequest request) {
    return service.updateVisit(id, x, request);
  }

  @GetMapping("/visits/{visitId}/diagnoses")
  public java.util.List<java.util.Map<String, Object>> diagnoses(
      @PathVariable long visitId, HttpServletRequest request) {
    return service.diagnoses(visitId, request);
  }

  @PostMapping("/visits/{visitId}/diagnoses")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> createDiagnosis(
      @PathVariable long visitId, @Valid @RequestBody DiagnosisRequest x, HttpServletRequest request) {
    return service.createDiagnosis(visitId, x, request);
  }

  @PutMapping("/diagnoses/{id}")
  public java.util.Map<String, Object> updateDiagnosis(
      @PathVariable long id, @Valid @RequestBody DiagnosisRequest x, HttpServletRequest request) {
    return service.updateDiagnosis(id, x, request);
  }

  @DeleteMapping("/diagnoses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDiagnosis(@PathVariable long id, HttpServletRequest request) {
    service.deleteDiagnosis(id, request);
  }

  @PostMapping("/prescriptions")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> createPrescription(
      @Valid @RequestBody PrescriptionRequest x, HttpServletRequest request) {
    return service.createPrescription(x, request);
  }

  @GetMapping("/prescriptions/{id}")
  public java.util.Map<String, Object> prescription(@PathVariable long id, HttpServletRequest request) {
    return service.prescription(id, request);
  }

  @PutMapping("/prescriptions/{id}/status")
  public java.util.Map<String, Object> prescriptionStatus(
      @PathVariable long id, @Valid @RequestBody Status x, HttpServletRequest request) {
    return service.updatePrescriptionStatus(id, x.status(), request);
  }

  /**
   *向处方中添加药品
   */
  @PostMapping("/prescriptions/{prescriptionId}/items")
  @ResponseStatus(HttpStatus.CREATED)
  public java.util.Map<String, Object> addItem(
      @PathVariable long prescriptionId, @Valid @RequestBody PrescriptionItemRequest x, HttpServletRequest request) {
    return service.addItem(prescriptionId, x, request);
  }

  /**
   * 修改处方中的药品
   */
  @PutMapping("/prescription-items/{prescription_item_id}")
  public java.util.Map<String, Object> updateItem(
      @PathVariable long prescription_item_id, @Valid @RequestBody PrescriptionItemRequest x, HttpServletRequest request) {
    return service.updateItem(prescription_item_id, x, request);
  }

  /**
   * 删除处方中的药品
   */
  @DeleteMapping("/prescription-items/{prescription_item_id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItem(@PathVariable long prescription_item_id, HttpServletRequest request) {
    service.deleteItem(prescription_item_id, request);
  }

  /** 处方状态更新请求。 */
  public record Status(@NotNull @Min(1) @Max(3) Integer status) {}
}
