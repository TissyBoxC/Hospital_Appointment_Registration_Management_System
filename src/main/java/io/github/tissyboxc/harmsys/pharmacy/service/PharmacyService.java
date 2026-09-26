package io.github.tissyboxc.harmsys.pharmacy.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 药房处方查询和发药业务逻辑。 */
public interface PharmacyService {
public List<Map<String, Object>> prescriptions(AuthenticatedUser operator, Integer status, Integer paymentStatus);

  public Map<String, Object> prescription(AuthenticatedUser operator, long id);

  public void dispense(AuthenticatedUser operator, long id, String ipAddress);
}
