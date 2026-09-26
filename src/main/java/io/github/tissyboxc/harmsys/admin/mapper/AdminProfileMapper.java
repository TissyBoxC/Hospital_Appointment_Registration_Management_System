package io.github.tissyboxc.harmsys.admin.mapper;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 管理员维护患者和医生资料的数据访问。 */
public interface AdminProfileMapper {

  Map<String, Object> selectPatient(@Param("id") long id);

  int updatePatient(
      @Param("id") long id, @Param("request") AdminPatientUpdateRequest request);

  Map<String, Object> selectPatientAfterUpdate(@Param("id") long id);

  Map<String, Object> selectDoctor(@Param("id") long id);

  int updateDoctor(
      @Param("id") long id, @Param("request") AdminDoctorUpdateRequest request);

  Map<String, Object> selectDoctorAfterUpdate(@Param("id") long id);

  int updateDoctorStatus(@Param("id") long id, @Param("status") int status);
}
