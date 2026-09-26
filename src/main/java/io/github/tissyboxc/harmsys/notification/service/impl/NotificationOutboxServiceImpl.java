package io.github.tissyboxc.harmsys.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.notification.entity.NotificationOutbox;
import io.github.tissyboxc.harmsys.notification.mapper.NotificationOutboxMapper;
import io.github.tissyboxc.harmsys.notification.service.NotificationOutboxService;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 外部通知队列业务实现。 */
@Service
public class NotificationOutboxServiceImpl implements NotificationOutboxService {
  private final NotificationOutboxMapper outboxMapper;
  private final OperationLogMapper operationLogMapper;

  public NotificationOutboxServiceImpl(
      NotificationOutboxMapper outboxMapper, OperationLogMapper operationLogMapper) {
    this.outboxMapper = outboxMapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(AuthenticatedUser user, Integer status, int limit) {
    requireAdmin(user);
    if (status != null && (status < 0 || status > 2))
      throw new UserRegistrationException(422, "队列状态只能为0到2");
    LambdaQueryWrapper<NotificationOutbox> wrapper = new LambdaQueryWrapper<>();
    if (status != null) wrapper.eq(NotificationOutbox::getStatus, status);
    wrapper.orderByDesc(NotificationOutbox::getId).last("LIMIT " + Math.min(Math.max(1, limit), 500));
    return outboxMapper.selectList(wrapper).stream().map(this::toResult).toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void send(AuthenticatedUser user, long id, String ipAddress) {
    requireAdmin(user);
    NotificationOutbox outbox = outboxMapper.selectById(id);
    if (outbox == null || (outbox.getStatus() != 0 && outbox.getStatus() != 2))
      throw new UserRegistrationException(409, "队列消息不存在或当前不可发送");
    outbox.setStatus(1);
    outbox.setSentAt(LocalDateTime.now());
    outbox.setErrorMessage(null);
    outboxMapper.updateById(outbox);
    writeLog(user.user_id(), "SEND_NOTIFICATION_OUTBOX", id, "模拟发送外部通知", ipAddress);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void retry(AuthenticatedUser user, long id, String ipAddress) {
    requireAdmin(user);
    NotificationOutbox outbox = outboxMapper.selectById(id);
    if (outbox == null || outbox.getStatus() != 2)
      throw new UserRegistrationException(409, "只有失败消息可以重试");
    outbox.setStatus(0);
    outbox.setErrorMessage(null);
    outbox.setSentAt(null);
    outboxMapper.updateById(outbox);
    writeLog(user.user_id(), "RETRY_NOTIFICATION_OUTBOX", id, "重新加入外部通知发送队列", ipAddress);
  }

  private void writeLog(
      long userId, String type, long id, String description, String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType("notification_outbox");
    log.setTargetId(id);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private Map<String, Object> toResult(NotificationOutbox outbox) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", outbox.getId());
    result.put("user_id", outbox.getUserId());
    result.put("channel", outbox.getChannel());
    result.put("recipient", outbox.getRecipient());
    result.put("subject", outbox.getSubject());
    result.put("content", outbox.getContent());
    result.put("status", outbox.getStatus());
    result.put("error_message", outbox.getErrorMessage());
    result.put("created_at", outbox.getCreatedAt());
    result.put("sent_at", outbox.getSentAt());
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以管理外部通知队列");
  }
}
