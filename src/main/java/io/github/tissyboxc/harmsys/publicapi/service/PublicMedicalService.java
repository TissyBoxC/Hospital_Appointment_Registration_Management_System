package io.github.tissyboxc.harmsys.publicapi.service;

import java.util.List;
import java.util.Map;

/** 公开医生、科室与排班查询业务逻辑。 */
public interface PublicMedicalService {
public List<Map<String, Object>> doctors(Long departmentId, String keyword);

  public Map<String, Object> doctor(long id);

  public List<Map<String, Object>> schedules(Long departmentId, Long doctorId, String scheduleDate, Integer period);

  public Map<String, Object> schedule(long id);

  public List<Map<String, Object>> slots(long scheduleId);
}
