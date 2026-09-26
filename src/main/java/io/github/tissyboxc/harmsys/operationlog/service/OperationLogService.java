package io.github.tissyboxc.harmsys.operationlog.service;

import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 操作日志查询业务。 */
public interface OperationLogService {

  List<Map<String, Object>> list(
      AuthenticatedUser user,
      Long userId,
      String operationType,
      String targetType,
      Long targetId,
      String startTime,
      String endTime,
      int limit);

  Map<String, Object> get(AuthenticatedUser user, long id);

  Map<String, Object> paged(AuthenticatedUser user, int page, int pageSize);
}
