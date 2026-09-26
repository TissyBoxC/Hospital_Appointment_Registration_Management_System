package io.github.tissyboxc.harmsys.department.mapper;

import io.github.tissyboxc.harmsys.department.dto.DepartmentDoctorUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 科室负责人范围内医生、排班和患者查询的数据访问。 */
public interface DepartmentManagerMapper {

  List<Long> selectManagedDepartmentIds(@Param("userId") long userId);

  List<Long> selectAllDepartmentIds();

  List<Map<String, Object>> selectDepartments(
      @Param("departmentIds") List<Long> departmentIds);

  List<Map<String, Object>> selectDoctors(
      @Param("departmentIds") List<Long> departmentIds, @Param("keyword") String keyword);

  Map<String, Object> selectDoctor(
      @Param("departmentIds") List<Long> departmentIds, @Param("doctorId") long doctorId);

  long countDoctorInDepartments(
      @Param("departmentIds") List<Long> departmentIds, @Param("doctorId") Long doctorId);

  int updateDoctor(
      @Param("doctorId") long doctorId,
      @Param("request") DepartmentDoctorUpdateRequest request);

  List<Map<String, Object>> selectSchedules(
      @Param("departmentIds") List<Long> departmentIds,
      @Param("doctorId") Long doctorId,
      @Param("scheduleDate") LocalDate scheduleDate);

  Map<String, Object> selectSchedule(
      @Param("departmentIds") List<Long> departmentIds, @Param("scheduleId") long scheduleId);

  List<Map<String, Object>> selectPatients(
      @Param("departmentIds") List<Long> departmentIds, @Param("keyword") String keyword);

  Map<String, Object> selectPatient(
      @Param("departmentIds") List<Long> departmentIds, @Param("patientId") long patientId);

  List<Map<String, Object>> selectPatientAppointments(
      @Param("departmentIds") List<Long> departmentIds, @Param("patientId") long patientId);

  List<Map<String, Object>> selectPatientVisits(
      @Param("departmentIds") List<Long> departmentIds, @Param("patientId") long patientId);
}
