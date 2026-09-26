package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理员预约查询业务逻辑。 */
public interface AdminAppointmentQueryService {
public List<Map<String, Object>> list(AuthenticatedUser operator);

  public Map<String, Object> page(AuthenticatedUser operator, int page, int pageSize, Integer status);

  public Map<String, Object> get(AuthenticatedUser operator, long id);
}
