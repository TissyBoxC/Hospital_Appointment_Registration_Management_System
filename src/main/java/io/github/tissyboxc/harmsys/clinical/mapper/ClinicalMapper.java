package io.github.tissyboxc.harmsys.clinical.mapper;

import io.github.tissyboxc.harmsys.clinical.entity.DiagnosisRecord;
import io.github.tissyboxc.harmsys.clinical.entity.Prescription;
import io.github.tissyboxc.harmsys.clinical.entity.PrescriptionItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 医生诊疗、诊断、处方和处方明细的数据访问。 */
public interface ClinicalMapper {

  List<Map<String, Object>> selectDoctorAppointments(@Param("doctorId") long doctorId);

  List<Map<String, Object>> selectDoctorAppointmentsPage(
      @Param("doctorId") long doctorId,
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countDoctorAppointments(
      @Param("doctorId") long doctorId, @Param("status") Integer status);

  Map<String, Object> selectDoctorAppointment(
      @Param("appointmentId") long appointmentId, @Param("doctorId") long doctorId);

  Map<String, Object> lockDoctorAppointment(
      @Param("appointmentId") long appointmentId, @Param("doctorId") long doctorId);

  long countEnabledDepartment(@Param("departmentId") long departmentId);

  int updatePatientName(
      @Param("patientId") long patientId, @Param("realName") String realName);

  int updateAppointment(
      @Param("appointmentId") long appointmentId,
      @Param("appointmentDate") Object appointmentDate,
      @Param("period") Object period,
      @Param("departmentId") Object departmentId,
      @Param("queueNo") Object queueNo,
      @Param("status") Object status,
      @Param("remark") Object remark);

  Map<String, Object> selectVisitByAppointment(@Param("appointmentId") long appointmentId);

  int insertVisit(
      @Param("appointmentId") long appointmentId,
      @Param("patientId") Object patientId,
      @Param("doctorId") Object doctorId,
      @Param("visitNo") String visitNo);

  int startExistingVisit(@Param("visitId") Object visitId);

  int updateAppointmentStatus(
      @Param("appointmentId") long appointmentId, @Param("status") int status);

  int completeVisit(@Param("visitId") Object visitId);

  List<Map<String, Object>> selectDoctorVisits(@Param("doctorId") long doctorId);

  List<Map<String, Object>> selectPatientVisits(@Param("patientId") long patientId);

  Map<String, Object> selectVisitDetail(@Param("visitId") long visitId);

  Map<String, Object> selectDoctorVisit(
      @Param("visitId") long visitId, @Param("doctorId") long doctorId);

  int updateVisit(
      @Param("visitId") long visitId,
      @Param("chiefComplaint") String chiefComplaint,
      @Param("presentIllness") String presentIllness,
      @Param("medicalAdvice") String medicalAdvice);

  List<Map<String, Object>> selectDiagnoses(@Param("visitId") long visitId);

  int insertDiagnosis(DiagnosisRecord diagnosis);

  Map<String, Object> selectDiagnosis(@Param("id") long id);

  Map<String, Object> selectDoctorDiagnosis(
      @Param("id") long id, @Param("doctorId") long doctorId);

  int updateDiagnosis(
      @Param("id") long id,
      @Param("diagnosisName") String diagnosisName,
      @Param("diagnosisCode") String diagnosisCode,
      @Param("diagnosisType") Integer diagnosisType,
      @Param("remark") String remark);

  int deleteDiagnosis(@Param("id") long id);

  int insertPrescription(Prescription prescription);

  Map<String, Object> selectPrescription(@Param("prescriptionId") long prescriptionId);

  Map<String, Object> selectVisitOwner(@Param("visitId") long visitId);

  List<Map<String, Object>> selectPrescriptionItems(
      @Param("prescriptionId") long prescriptionId);

  Map<String, Object> lockDoctorPrescription(
      @Param("prescriptionId") long prescriptionId, @Param("doctorId") long doctorId);

  BigDecimal selectPrescriptionAmount(@Param("prescriptionId") long prescriptionId);

  int submitPrescription(
      @Param("prescriptionId") long prescriptionId, @Param("amount") BigDecimal amount);

  int resetPrescription(@Param("prescriptionId") long prescriptionId);

  Map<String, Object> lockPatientPrescription(@Param("prescriptionId") long prescriptionId);

  int markPrescriptionPaid(
      @Param("prescriptionId") long prescriptionId,
      @Param("paymentNo") String paymentNo,
      @Param("amount") BigDecimal amount);

  int insertPrescriptionItem(PrescriptionItem item);

  Map<String, Object> selectPrescriptionItem(@Param("id") long id);

  Map<String, Object> selectEditablePrescriptionItem(
      @Param("itemId") long itemId, @Param("doctorId") long doctorId);

  int deletePrescriptionItem(@Param("itemId") long itemId);

  int updatePrescriptionItem(
      @Param("itemId") long itemId,
      @Param("medicineId") Long medicineId,
      @Param("drugName") String drugName,
      @Param("specification") String specification,
      @Param("dosage") String dosage,
      @Param("frequency") String frequency,
      @Param("days") Integer days,
      @Param("quantity") BigDecimal quantity,
      @Param("unitPrice") BigDecimal unitPrice,
      @Param("remark") String remark);

  Map<String, Object> selectAvailableMedicine(@Param("medicineId") long medicineId);
}
