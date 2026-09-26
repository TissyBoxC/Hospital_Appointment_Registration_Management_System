package io.github.tissyboxc.harmsys.clinical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.clinical.entity.MedicalAttachment;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** medical_attachment 表数据访问。 */
public interface MedicalAttachmentMapper extends BaseMapper<MedicalAttachment> {

  Map<String, Object> selectDetail(@Param("id") long id);

  List<Map<String, Object>> selectByPatient(
      @Param("patientId") long patientId, @Param("visitId") Long visitId);

  long countActivePatient(@Param("patientId") long patientId);
}
