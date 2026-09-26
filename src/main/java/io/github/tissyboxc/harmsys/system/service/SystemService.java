package io.github.tissyboxc.harmsys.system.service;

import io.github.tissyboxc.harmsys.system.repository.SystemRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 系统监控、通知与配置业务逻辑。 */
@Service
public class SystemService {
  private final SystemRepository repository;

  public SystemService(SystemRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> health() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "UP");
    result.put("database", repository.databaseCheck());
    result.put("time", OffsetDateTime.now());
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> metrics(AuthenticatedUser user) {
    requireAdmin(user);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("users", repository.count("sys_user", "deleted=0"));
    result.put("patients", repository.count("patient", "deleted=0"));
    result.put("doctors", repository.count("doctor", "deleted=0"));
    result.put("active_schedules", repository.count("doctor_schedule", "status=1"));
    result.put("active_appointments", repository.count("appointment", "status IN (1,2,3,4)"));
    result.put("pending_outbox", repository.count("notification_outbox", "status=0"));
    result.put(
        "active_sessions", repository.count("active_session", "expires_at>CURRENT_TIMESTAMP"));
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> notifications(AuthenticatedUser user) {
    return repository.notifications(user.user_id());
  }

  @Transactional(rollbackFor = Exception.class)
  public void read(AuthenticatedUser user, long id) {
    if (repository.markNotificationRead(id, user.user_id()) != 1)
      throw new UserRegistrationException(404, "通知不存在");
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> configs(AuthenticatedUser user) {
    requireAdmin(user);
    return repository.configs();
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateConfig(
      AuthenticatedUser user, String key, Map<String, Object> body) {
    requireAdmin(user);
    Object value = body.get("config_value");
    if (value == null) throw new UserRegistrationException(422, "config_value不能为空");
    repository.upsertConfig(key, String.valueOf(value), body.get("description"));
    return repository.findConfig(key);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以管理系统参数");
  }
}

