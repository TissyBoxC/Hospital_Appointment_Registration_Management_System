package io.github.tissyboxc.harmsys.publicapi.service;

import io.github.tissyboxc.harmsys.publicapi.repository.PublicSchedulePageRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公开可预约排班分页业务逻辑。 */
@Service
public class PublicSchedulePageService {
  private final PublicSchedulePageRepository repository;

  public PublicSchedulePageService(PublicSchedulePageRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> page(
      int page, int pageSize, Long departmentId, Long doctorId) {
    int currentPage = Math.max(1, page);
    int size = Math.min(Math.max(1, pageSize), 100);
    int offset = (currentPage - 1) * size;
    List<Map<String, Object>> items =
        repository.page(departmentId, doctorId, size, offset);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", currentPage);
    result.put("page_size", size);
    result.put("total", repository.total(departmentId, doctorId));
    result.put("items", items);
    return result;
  }
}
