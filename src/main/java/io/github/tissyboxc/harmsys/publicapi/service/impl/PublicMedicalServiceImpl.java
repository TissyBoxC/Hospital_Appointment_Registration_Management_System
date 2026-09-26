package io.github.tissyboxc.harmsys.publicapi.service.impl;

import io.github.tissyboxc.harmsys.publicapi.service.PublicMedicalService;

import io.github.tissyboxc.harmsys.publicapi.mapper.PublicMedicalMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公开医生、科室与排班查询业务逻辑。 */
@Service
public class PublicMedicalServiceImpl implements PublicMedicalService {
  private final PublicMedicalMapper mapper;

  public PublicMedicalServiceImpl(PublicMedicalMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> doctors(Long departmentId, String keyword) {
    return mapper.selectDoctors(departmentId, keyword == null ? null : keyword.trim());
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> doctor(long id) {
    return mapper.selectDoctor(id);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> schedules(
      Long departmentId, Long doctorId, String scheduleDate, Integer period) {
    return mapper.selectSchedules(departmentId, doctorId, scheduleDate, period);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> schedule(long id) {
    return mapper.selectSchedule(id);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> slots(long scheduleId) {
    return mapper.selectSlots(scheduleId);
  }
}
