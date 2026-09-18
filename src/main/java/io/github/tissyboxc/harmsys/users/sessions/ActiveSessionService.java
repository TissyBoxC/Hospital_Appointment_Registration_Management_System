package io.github.tissyboxc.harmsys.users.sessions;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
/** 将登录会话登记到数据库，禁用账号后会话立即失效。 */
public class ActiveSessionService {
  private final JdbcTemplate jdbc;

  public ActiveSessionService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 登陆成功后登记新会话信息
   * @param sessionId 会话SessionId
   * @param userId 用户ID
   * @param timeoutSeconds 过期时间
   */
  public void register(String sessionId, long userId, int timeoutSeconds) {
    jdbc.update(
        "INSERT INTO active_session(session_id,user_id,expires_at)"
            + " VALUES(?,?,DATE_ADD(NOW(),INTERVAL ? SECOND)) ON DUPLICATE KEY UPDATE"
            + " user_id=VALUES(user_id),last_seen_at=CURRENT_TIMESTAMP,expires_at=VALUES(expires_at)",
        sessionId,
        userId,
        timeoutSeconds);
  }

  /**
   *账户合法性校验,验证会话未过期+用户账户启用且未被删除+验证该Session的用户和该SessionId存在
   * @param sessionId 会话SessionId
   * @param userId 用户ID
   * @return
   */
  public boolean valid(String sessionId, long userId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM active_session s JOIN sys_user u ON u.id=s.user_id WHERE"
                + " s.session_id=? AND s.user_id=? AND u.status=1 AND u.deleted=0 AND"
                + " s.expires_at>CURRENT_TIMESTAMP",
            Long.class,
            sessionId,
            userId)
        > 0;
  }

  /**
   * 记录活动会话,更新时间
   */
  public void touch(String sessionId) {
    jdbc.update(
        "UPDATE active_session SET last_seen_at=CURRENT_TIMESTAMP WHERE session_id=?", sessionId);
  }

  /**
   * 删除单个会话,登出
   */
  public void invalidate(String sessionId) {
    jdbc.update("DELETE FROM active_session WHERE session_id=?", sessionId);
  }

  /**
   * 删除某个用户的全部记录(禁用账号,后台重置密码)
   * @param userId 用户ID
   */
  public void invalidateUser(long userId) {
    jdbc.update("DELETE FROM active_session WHERE user_id=?", userId);
  }
}
