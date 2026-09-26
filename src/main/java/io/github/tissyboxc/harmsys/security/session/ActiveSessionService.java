package io.github.tissyboxc.harmsys.security.session;

/** 活跃登录会话业务。 */
public interface ActiveSessionService {

  void register(String sessionId, long userId, int timeoutSeconds);

  boolean valid(String sessionId, long userId);

  void touch(String sessionId);

  void invalidate(String sessionId);

  void invalidateUser(long userId);
}
