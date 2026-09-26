package io.github.tissyboxc.harmsys.notification.service;

import io.github.tissyboxc.harmsys.notification.repository.NotificationOutboxRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 外部通知队列业务逻辑。 */
@Service
public class NotificationOutboxService {
  private final NotificationOutboxRepository repository;

  public NotificationOutboxService(NotificationOutboxRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(AuthenticatedUser user, Integer status, int limit) {
    requireAdmin(user);
    if (status != null && (status < 0 || status > 2))
      throw new UserRegistrationException(422, "队列状态只能为0到2");
    return repository.list(status, Math.min(Math.max(1, limit), 500));
  }

  @Transactional(rollbackFor = Exception.class)
  public void send(AuthenticatedUser user, long id, String ipAddress) {
    requireAdmin(user);
    if (repository.markSent(id) != 1)
      throw new UserRegistrationException(409, "队列消息不存在或当前不可发送");
    repository.insertOperationLog(
        user.user_id(), "SEND_NOTIFICATION_OUTBOX", id, "模拟发送外部通知", ipAddress);
  }

  @Transactional(rollbackFor = Exception.class)
  public void retry(AuthenticatedUser user, long id, String ipAddress) {
    requireAdmin(user);
    if (repository.retry(id) != 1)
      throw new UserRegistrationException(409, "只有失败消息可以重试");
    repository.insertOperationLog(
        user.user_id(), "RETRY_NOTIFICATION_OUTBOX", id, "重新加入外部通知发送队列", ipAddress);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以管理外部通知队列");
  }
}

