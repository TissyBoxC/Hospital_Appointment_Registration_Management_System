package io.github.tissyboxc.harmsys.security.session;

import org.springframework.stereotype.Service;

@Service
/** 将登录会话登记到数据库，禁用账号后会话立即失效。 */
public class ActiveSessionService {
  private final ActiveSessionRepository repository;

  public ActiveSessionService(ActiveSessionRepository repository) {
    this.repository = repository;
  }

  /**
   * 登陆成功后登记新会话信息
   * @param sessionId 会话SessionId
   * @param userId 用户ID
   * @param timeoutSeconds 过期时间
   */
  public void register(String sessionId, long userId, int timeoutSeconds) {
    repository.register(sessionId, userId, timeoutSeconds);
  }

  /**
   *账户合法性校验,验证会话未过期+用户账户启用且未被删除+验证该Session的用户和该SessionId存在
   * @param sessionId 会话SessionId
   * @param userId 用户ID
   * @return
   */
  public boolean valid(String sessionId, long userId) {
    return repository.valid(sessionId, userId);
  }

  /**
   * 记录活动会话,更新时间
   */
  public void touch(String sessionId) {
    repository.touch(sessionId);
  }

  /**
   * 删除单个会话,登出
   */
  public void invalidate(String sessionId) {
    repository.invalidate(sessionId);
  }

  /**
   * 删除某个用户的全部记录(禁用账号,后台重置密码)
   * @param userId 用户ID
   */
  public void invalidateUser(long userId) {
    repository.invalidateUser(userId);
  }
}

