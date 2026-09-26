package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.appointment.repository.AdminAppointmentRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员预约查询业务逻辑。 */
@Service
public class AdminAppointmentQueryService {
  private final AdminAppointmentRepository repository;

  public AdminAppointmentQueryService(AdminAppointmentRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(AuthenticatedUser operator) {
    requireAdmin(operator);
    return repository.listAll();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> page(AuthenticatedUser operator, int page, int pageSize, Integer status) {
    requireAdmin(operator);
    int normalizedPage = Math.max(1, page);
    int normalizedSize = Math.min(Math.max(1, pageSize), 100);
    int offset = (normalizedPage - 1) * normalizedSize;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", normalizedPage);
    result.put("page_size", normalizedSize);
    result.put("total", repository.total(status));
    result.put("items", repository.page(status, normalizedSize, offset));
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get(AuthenticatedUser operator, long id) {
    requireAdmin(operator);
    Map<String, Object> result = repository.detail(id);
    if (result == null) throw new UserRegistrationException(404, "预约不存在");
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以查询预约");
  }
}

