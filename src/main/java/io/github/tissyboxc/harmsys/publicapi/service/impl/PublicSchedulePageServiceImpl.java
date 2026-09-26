package io.github.tissyboxc.harmsys.publicapi.service.impl;

import io.github.tissyboxc.harmsys.publicapi.service.PublicSchedulePageService;

import io.github.tissyboxc.harmsys.publicapi.mapper.PublicSchedulePageMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公开可预约排班分页业务逻辑。 */
@Service
public class PublicSchedulePageServiceImpl implements PublicSchedulePageService {
  private final PublicSchedulePageMapper mapper;

  public PublicSchedulePageServiceImpl(PublicSchedulePageMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> page(
      int page, int pageSize, Long departmentId, Long doctorId) {
    int currentPage = Math.max(1, page);
    int size = Math.min(Math.max(1, pageSize), 100);
    int offset = (currentPage - 1) * size;
    List<Map<String, Object>> items =
        mapper.selectPage(departmentId, doctorId, size, offset);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", currentPage);
    result.put("page_size", size);
    result.put("total", mapper.countPage(departmentId, doctorId));
    result.put("items", items);
    return result;
  }
}
