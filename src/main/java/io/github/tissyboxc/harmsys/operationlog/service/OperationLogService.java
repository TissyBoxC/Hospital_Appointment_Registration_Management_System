package io.github.tissyboxc.harmsys.operationlog.service;

import io.github.tissyboxc.harmsys.operationlog.repository.OperationLogRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 操作日志查询业务逻辑。 */
@Service
public class OperationLogService {
  private final OperationLogRepository repository;

  public OperationLogService(OperationLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(
      AuthenticatedUser user,
      Long userId,
      String operationType,
      String targetType,
      Long targetId,
      String startTime,
      String endTime,
      int limit) {
    requireAdmin(user);
    return repository.list(
        userId,
        operationType,
        targetType,
        targetId,
        startTime,
        endTime,
        Math.max(1, Math.min(limit, 500)));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get(AuthenticatedUser user, long id) {
    requireAdmin(user);
    Map<String, Object> result = repository.findById(id);
    if (result == null) throw new UserRegistrationException(404, "操作日志不存在");
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> paged(AuthenticatedUser user, int page, int pageSize) {
    requireAdmin(user);
    int size = Math.max(1, Math.min(pageSize, 100));
    int currentPage = Math.max(1, page);
    int offset = (currentPage - 1) * size;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", currentPage);
    result.put("page_size", size);
    result.put("total", repository.count());
    result.put("items", repository.page(size, offset));
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以查询操作日志");
  }
}

