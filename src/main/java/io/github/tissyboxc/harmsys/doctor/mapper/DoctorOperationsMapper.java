package io.github.tissyboxc.harmsys.doctor.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 医生工作台患者、预约、就诊、诊断和处方查询。 */
public interface DoctorOperationsMapper {

  List<Map<String, Object>> selectPatients(
      @Param("doctorId") long doctorId, @Param("keyword") String keyword);

  Map<String, Object> selectPatient(
      @Param("patientId") long patientId, @Param("doctorId") long doctorId);

  List<Map<String, Object>> selectAppointments(
      @Param("patientId") long patientId, @Param("doctorId") long doctorId);

  List<Map<String, Object>> selectVisits(
      @Param("patientId") long patientId, @Param("doctorId") long doctorId);

  List<Map<String, Object>> selectDiagnoses(
      @Param("patientId") long patientId, @Param("doctorId") long doctorId);

  List<Map<String, Object>> selectPrescriptions(
      @Param("patientId") long patientId, @Param("doctorId") long doctorId);
}
