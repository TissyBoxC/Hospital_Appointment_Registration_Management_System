package io.github.tissyboxc.harmsys.security.session.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.security.session.ActiveSessionService;
import io.github.tissyboxc.harmsys.security.session.entity.ActiveSession;
import io.github.tissyboxc.harmsys.security.session.mapper.ActiveSessionMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 活跃登录会话业务实现。 */
@Service
public class ActiveSessionServiceImpl implements ActiveSessionService {
  private final ActiveSessionMapper mapper;

  public ActiveSessionServiceImpl(ActiveSessionMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void register(String sessionId, long userId, int timeoutSeconds) {
    ActiveSession existing = mapper.selectById(sessionId);
    LocalDateTime now = LocalDateTime.now();
    if (existing == null) {
      ActiveSession session = new ActiveSession();
      session.setSessionId(sessionId);
      session.setUserId(userId);
      session.setLastSeenAt(now);
      session.setExpiresAt(now.plusSeconds(timeoutSeconds));
      mapper.insert(session);
      return;
    }
    existing.setUserId(userId);
    existing.setLastSeenAt(now);
    existing.setExpiresAt(now.plusSeconds(timeoutSeconds));
    mapper.updateById(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean valid(String sessionId, long userId) {
    return mapper.countValid(sessionId, userId) > 0;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void touch(String sessionId) {
    ActiveSession session = mapper.selectById(sessionId);
    if (session == null) return;
    session.setLastSeenAt(LocalDateTime.now());
    mapper.updateById(session);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void invalidate(String sessionId) {
    mapper.deleteById(sessionId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void invalidateUser(long userId) {
    mapper.delete(new LambdaQueryWrapper<ActiveSession>().eq(ActiveSession::getUserId, userId));
  }
}
