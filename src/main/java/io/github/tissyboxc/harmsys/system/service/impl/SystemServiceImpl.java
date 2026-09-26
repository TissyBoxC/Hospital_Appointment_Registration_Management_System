package io.github.tissyboxc.harmsys.system.service.impl;

import io.github.tissyboxc.harmsys.system.service.SystemService;

import io.github.tissyboxc.harmsys.system.mapper.SystemMapper;
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
public class SystemServiceImpl implements SystemService {
  private final SystemMapper mapper;

  public SystemServiceImpl(SystemMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> health() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "UP");
    result.put("database", mapper.selectDatabaseCheck());
    result.put("time", OffsetDateTime.now());
    return result;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> metrics(AuthenticatedUser user) {
    requireAdmin(user);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("users", mapper.countUsers());
    result.put("patients", mapper.countPatients());
    result.put("doctors", mapper.countDoctors());
    result.put("active_schedules", mapper.countActiveSchedules());
    result.put("active_appointments", mapper.countActiveAppointments());
    result.put("pending_outbox", mapper.countPendingOutbox());
    result.put("active_sessions", mapper.countActiveSessions());
    return result;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> notifications(AuthenticatedUser user) {
    return mapper.selectNotifications(user.user_id());
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void read(AuthenticatedUser user, long id) {
    if (mapper.markNotificationRead(id, user.user_id()) != 1)
      throw new UserRegistrationException(404, "通知不存在");
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> configs(AuthenticatedUser user) {
    requireAdmin(user);
    return mapper.selectConfigs();
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateConfig(
      AuthenticatedUser user, String key, Map<String, Object> body) {
    requireAdmin(user);
    Object value = body.get("config_value");
    if (value == null) throw new UserRegistrationException(422, "config_value不能为空");
    mapper.upsertConfig(key, String.valueOf(value), body.get("description"));
    return mapper.selectConfig(key);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以管理系统参数");
  }
}

