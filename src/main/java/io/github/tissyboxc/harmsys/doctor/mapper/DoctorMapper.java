package io.github.tissyboxc.harmsys.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.doctor.dto.DoctorProfileResult;
import io.github.tissyboxc.harmsys.doctor.entity.Doctor;
import org.apache.ibatis.annotations.Param;

/** doctor 表数据访问。 */
public interface DoctorMapper extends BaseMapper<Doctor> {

  DoctorProfileResult selectProfile(@Param("doctorId") long doctorId);
}
