package io.github.tissyboxc.harmsys.registration.service;

import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 挂号员工作台业务逻辑。 */
public interface RegistrationOperationsService {
public List<Map<String, Object>> patients(AuthenticatedUser operator, String keyword);

  public Map<String, Object> patient(AuthenticatedUser operator, long id);

  public Map<String, Object> create(AuthenticatedUser operator, Map<String, Object> body, String ipAddress);

  public void refund(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> callNext(AuthenticatedUser operator, long id, String ipAddress);

  public void noShow(AuthenticatedUser operator, long id, String ipAddress);

  public Map<String, Object> requeue(AuthenticatedUser operator, long id, String ipAddress);
}
