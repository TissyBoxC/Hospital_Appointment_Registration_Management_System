package io.github.tissyboxc.harmsys.payment.service;

import io.github.tissyboxc.harmsys.payment.repository.PaymentQueryRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 支付记录查询业务逻辑。 */
@Service
public class PaymentQueryService {
  private final PaymentQueryRepository repository;

  public PaymentQueryService(PaymentQueryRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> patientPayments(
      AuthenticatedUser user, int page, int pageSize) {
    long patientId = requirePatient(user);
    int size = Math.min(Math.max(pageSize, 1), 100);
    int currentPage = Math.max(page, 1);
    int offset = (currentPage - 1) * size;
    return result(
        currentPage,
        size,
        repository.countPatientPayments(patientId),
        repository.patientPayments(patientId, size, offset));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> appointmentPayment(
      AuthenticatedUser user, long appointmentId) {
    Map<String, Object> payment =
        repository.appointmentPayment(appointmentId, requirePatient(user));
    if (payment == null) throw new UserRegistrationException(404, "该预约没有支付记录");
    return payment;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> adminPayments(
      AuthenticatedUser user, int page, int pageSize, Integer status) {
    requireAdmin(user);
    int size = Math.min(Math.max(pageSize, 1), 100);
    int currentPage = Math.max(page, 1);
    int offset = (currentPage - 1) * size;
    return result(
        currentPage,
        size,
        repository.countAdminPayments(status),
        repository.adminPayments(status, size, offset));
  }

  private Map<String, Object> result(
      int page, int size, long total, List<Map<String, Object>> items) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", page);
    result.put("page_size", size);
    result.put("total", total);
    result.put("items", items);
    return result;
  }

  private long requirePatient(AuthenticatedUser user) {
    if (user.patient_id() == null
        || user.role_codes().stream().noneMatch("PATIENT"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return user.patient_id();
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以查询支付记录");
  }
}

