package io.github.tissyboxc.harmsys.users.repository;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 登录失败次数和锁定状态持久化访问。 */
@Repository
public class LoginAttemptRepository {
  private final JdbcTemplate jdbc;

  public LoginAttemptRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean isLocked(String username, String ipAddress) {
    Timestamp lockedUntil =
        jdbc.query(
            "SELECT locked_until FROM login_attempt WHERE username=? AND ip_address=?",
            rs -> rs.next() ? rs.getTimestamp(1) : null,
            username,
            ipAddress);
    return lockedUntil != null && lockedUntil.toInstant().isAfter(java.time.Instant.now());
  }

  public void recordFailure(String username, String ipAddress) {
    jdbc.update(
        "INSERT INTO login_attempt(username,ip_address,fail_count,locked_until,last_attempt_at)"
            + " VALUES(?,?,1,NULL,CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE"
            + " fail_count=fail_count+1,locked_until=CASE WHEN fail_count+1>=5 THEN"
            + " DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE) ELSE locked_until"
            + " END,last_attempt_at=CURRENT_TIMESTAMP",
        username,
        ipAddress);
  }

  public void clear(String username, String ipAddress) {
    jdbc.update(
        "DELETE FROM login_attempt WHERE username=? AND ip_address=?", username, ipAddress);
  }
}
