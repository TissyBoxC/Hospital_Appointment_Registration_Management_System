package io.github.tissyboxc.harmsys.payment.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/** 处理模拟支付、退款和支付状态查询的业务服务。 */
public interface PaymentService {
public Map<String, Object> get(long id, HttpServletRequest r);

  public Map<String, Object> pay(long appointmentId, HttpServletRequest r);

  public void refund(long paymentId, HttpServletRequest r);

  public void refund(long paymentId, HttpServletRequest r, String reason);
}
