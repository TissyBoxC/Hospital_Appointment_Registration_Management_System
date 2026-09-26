package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import java.util.Objects;

/** 管理员预约修改、迁移、取消与过期业务逻辑。 */
public interface AdminAppointmentOperationsService {
public Map<String, Object> update(AuthenticatedUser operator, long id, Map<String, Object> body, String ipAddress);

  public void cancel(AuthenticatedUser operator, long id, String reason, String ipAddress);

  public void expire(AuthenticatedUser operator, long id, String ipAddress);
}
