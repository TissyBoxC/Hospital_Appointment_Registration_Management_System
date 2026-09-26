package io.github.tissyboxc.harmsys.system.service;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 系统监控、通知与配置业务逻辑。 */
public interface SystemService {
public Map<String, Object> health();

  public Map<String, Object> metrics(AuthenticatedUser user);

  public List<Map<String, Object>> notifications(AuthenticatedUser user);

  public void read(AuthenticatedUser user, long id);

  public List<Map<String, Object>> configs(AuthenticatedUser user);

  public Map<String, Object> updateConfig(AuthenticatedUser user, String key, Map<String, Object> body);
}
