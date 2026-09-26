package io.github.tissyboxc.harmsys.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.patient.entity.Patient;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** patient 表数据访问。 */
public interface PatientMapper extends BaseMapper<Patient> {

  @Select(
      "SELECT id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,"
          + "emergency_phone FROM patient WHERE id=#{patientId} AND deleted=0")
  Map<String, Object> selectProfile(@Param("patientId") long patientId);
}
