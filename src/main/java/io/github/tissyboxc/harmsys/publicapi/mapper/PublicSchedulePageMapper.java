package io.github.tissyboxc.harmsys.publicapi.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 公开可预约排班分页查询的数据访问。 */
public interface PublicSchedulePageMapper {

  List<Map<String, Object>> selectPage(
      @Param("departmentId") Long departmentId,
      @Param("doctorId") Long doctorId,
      @Param("size") int size,
      @Param("offset") int offset);

  long countPage(
      @Param("departmentId") Long departmentId, @Param("doctorId") Long doctorId);
}
