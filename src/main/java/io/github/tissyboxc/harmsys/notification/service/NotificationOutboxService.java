package io.github.tissyboxc.harmsys.notification.service;

import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 外部通知队列业务。 */
public interface NotificationOutboxService {

  List<Map<String, Object>> list(AuthenticatedUser user, Integer status, int limit);

  void send(AuthenticatedUser user, long id, String ipAddress);

  void retry(AuthenticatedUser user, long id, String ipAddress);
}
