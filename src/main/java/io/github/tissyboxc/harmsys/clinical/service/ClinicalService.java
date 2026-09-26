package io.github.tissyboxc.harmsys.clinical.service;

import io.github.tissyboxc.harmsys.clinical.dto.*;
import io.github.tissyboxc.harmsys.clinical.entity.DiagnosisRecord;
import io.github.tissyboxc.harmsys.clinical.entity.Prescription;
import io.github.tissyboxc.harmsys.clinical.entity.PrescriptionItem;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/** 负责就诊、诊断和处方状态流转的业务服务。 */
public interface ClinicalService {
public List<Map<String, Object>> doctorAppointments(HttpServletRequest r);

  public Map<String, Object> doctorAppointmentsPage(HttpServletRequest r, int page, int size, Integer status);

  public Map<String, Object> doctorAppointment(long id, HttpServletRequest r);

  public Map<String, Object> updateDoctorAppointment(long appointmentId, DoctorAppointmentUpdateRequest x, HttpServletRequest r);

  public Map<String, Object> startVisit(long appointmentId, HttpServletRequest r);

  public Map<String, Object> completeVisit(long appointmentId, HttpServletRequest r);

  public List<Map<String, Object>> doctorVisits(HttpServletRequest r);

  public List<Map<String, Object>> patientVisits(HttpServletRequest r);

  public Map<String, Object> getVisit(long visit_id, HttpServletRequest r);

  public Map<String, Object> updateVisit(long visit_id, VisitUpdateRequest x, HttpServletRequest r);

  public List<Map<String, Object>> diagnoses(long visitId, HttpServletRequest r);

  public Map<String, Object> createDiagnosis(long visitId, DiagnosisRequest x, HttpServletRequest r);

  public Map<String, Object> updateDiagnosis(long id, DiagnosisRequest x, HttpServletRequest r);

  public void deleteDiagnosis(long diagnosis_id, HttpServletRequest r);

  public Map<String, Object> createPrescription(PrescriptionRequest x, HttpServletRequest request);

  public Map<String, Object> prescription(long prescriptionid, HttpServletRequest request);

  public Map<String, Object> updatePrescriptionStatus(long prescriptionId, int status, HttpServletRequest r);

  public Map<String, Object> payPrescription(long prescriptionId, HttpServletRequest request);

  public Map<String, Object> addItem(long prescriptionId, PrescriptionItemRequest x, HttpServletRequest r);

  public void deleteItem(long prescription_item_id, HttpServletRequest r);

  public Map<String, Object> updateItem(long prescription_item_id, PrescriptionItemRequest x, HttpServletRequest request);
}
