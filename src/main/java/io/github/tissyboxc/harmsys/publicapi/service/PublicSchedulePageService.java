package io.github.tissyboxc.harmsys.publicapi.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 公开可预约排班分页业务逻辑。 */
public interface PublicSchedulePageService {
public Map<String, Object> page(int page, int pageSize, Long departmentId, Long doctorId);
}
