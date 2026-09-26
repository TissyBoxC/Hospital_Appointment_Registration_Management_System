package io.github.tissyboxc.harmsys.doctor.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 排班停诊、状态变更和时间段维护业务逻辑。 */
public interface ScheduleLifecycleService {
public Map<String, Object> adminStop(AuthenticatedUser operator, long id, String reason, String ipAddress);

  public Map<String, Object> doctorStop(AuthenticatedUser operator, long id, String reason, String ipAddress);

  public Map<String, Object> updateStatus(AuthenticatedUser operator, long id, int status, String ipAddress);

  public Map<String, Object> updateSlot(AuthenticatedUser operator, long slotId, Map<String, Object> body, String ipAddress);

  public void deleteSlot(AuthenticatedUser operator, long slotId, String ipAddress);
}
