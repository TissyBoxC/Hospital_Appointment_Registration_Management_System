package io.github.tissyboxc.harmsys.clinical.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 患者医疗历史查询。 */
public interface PatientHistoryMapper {

  List<Map<String, Object>> selectPrescriptions(@Param("patientId") long patientId);

  long countOwnedPrescription(
      @Param("prescriptionId") long prescriptionId, @Param("patientId") long patientId);

  List<Map<String, Object>> selectPrescriptionItems(
      @Param("prescriptionId") long prescriptionId);

  List<Map<String, Object>> selectDiagnoses(@Param("patientId") long patientId);

  long countOwnedVisit(@Param("visitId") long visitId, @Param("patientId") long patientId);

  List<Map<String, Object>> selectDiagnosesByVisit(@Param("visitId") long visitId);
}
