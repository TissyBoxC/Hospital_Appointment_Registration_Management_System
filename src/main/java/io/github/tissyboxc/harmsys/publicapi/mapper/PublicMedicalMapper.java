package io.github.tissyboxc.harmsys.publicapi.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 公开医生、科室与排班查询的数据访问。 */
public interface PublicMedicalMapper {

  List<Map<String, Object>> selectDoctors(
      @Param("departmentId") Long departmentId, @Param("keyword") String keyword);

  Map<String, Object> selectDoctor(@Param("id") long id);

  List<Map<String, Object>> selectSchedules(
      @Param("departmentId") Long departmentId,
      @Param("doctorId") Long doctorId,
      @Param("scheduleDate") String scheduleDate,
      @Param("period") Integer period);

  Map<String, Object> selectSchedule(@Param("id") long id);

  List<Map<String, Object>> selectSlots(@Param("scheduleId") long scheduleId);
}
