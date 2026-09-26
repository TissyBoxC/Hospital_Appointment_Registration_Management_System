package io.github.tissyboxc.harmsys.system.service;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.system.repository.DataConsistencyRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业务数据一致性检查和修复业务逻辑。 */
@Service
public class DataConsistencyService {
  private final DataConsistencyRepository repository;

  public DataConsistencyService(DataConsistencyRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> check(AuthenticatedUser user) {
    requireAdmin(user);
    return repository.check();
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> repair(AuthenticatedUser user) {
    requireAdmin(user);
    int affected = repository.repair();
    repository.insertOperationLog(
        user.user_id(), "管理员执行数据一致性修复，共影响" + affected + "条记录");
    Map<String, Object> result = repository.check();
    result.put("affected_rows", affected);
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行数据一致性检查");
  }
}
