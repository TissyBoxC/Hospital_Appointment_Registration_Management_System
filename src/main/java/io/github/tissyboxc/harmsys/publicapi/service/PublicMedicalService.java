package io.github.tissyboxc.harmsys.publicapi.service;

import io.github.tissyboxc.harmsys.publicapi.repository.PublicMedicalRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公开医生、科室与排班查询业务逻辑。 */
@Service
public class PublicMedicalService {
  private final PublicMedicalRepository repository;

  public PublicMedicalService(PublicMedicalRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> doctors(Long departmentId, String keyword) {
    return repository.doctors(departmentId, keyword);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> doctor(long id) {
    return repository.doctor(id);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> schedules(
      Long departmentId, Long doctorId, String scheduleDate, Integer period) {
    return repository.schedules(departmentId, doctorId, scheduleDate, period);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> schedule(long id) {
    return repository.schedule(id);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> slots(long scheduleId) {
    return repository.slots(scheduleId);
  }
}
