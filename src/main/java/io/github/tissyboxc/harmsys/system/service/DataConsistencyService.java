package io.github.tissyboxc.harmsys.system.service;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import java.util.LinkedHashMap;
import java.util.Map;

/** 业务数据一致性检查和修复业务逻辑。 */
public interface DataConsistencyService {
public Map<String, Object> check(AuthenticatedUser user);

  public Map<String, Object> repair(AuthenticatedUser user);
}
