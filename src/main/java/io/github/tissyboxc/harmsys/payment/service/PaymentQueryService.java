package io.github.tissyboxc.harmsys.payment.service;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 支付记录查询业务逻辑。 */
public interface PaymentQueryService {
public Map<String, Object> patientPayments(AuthenticatedUser user, int page, int pageSize);

  public Map<String, Object> appointmentPayment(AuthenticatedUser user, long appointmentId);

  public Map<String, Object> adminPayments(AuthenticatedUser user, int page, int pageSize, Integer status);
}
