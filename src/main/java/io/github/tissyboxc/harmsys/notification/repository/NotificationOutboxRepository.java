package io.github.tissyboxc.harmsys.notification.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 通知发件箱持久化访问。 */
@Repository
public class NotificationOutboxRepository {
  private final JdbcTemplate jdbc;

  public NotificationOutboxRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(Integer status, int size) {
    if (status == null)
      return jdbc.queryForList(
          "SELECT * FROM notification_outbox ORDER BY id DESC LIMIT ?", size);
    return jdbc.queryForList(
        "SELECT * FROM notification_outbox WHERE status=? ORDER BY id DESC LIMIT ?", status, size);
  }

  public int markSent(long id) {
    return jdbc.update(
        "UPDATE notification_outbox SET status=1,sent_at=CURRENT_TIMESTAMP,error_message=NULL"
            + " WHERE id=? AND status IN (0,2)",
        id);
  }

  public int retry(long id) {
    return jdbc.update(
        "UPDATE notification_outbox SET status=0,error_message=NULL,sent_at=NULL WHERE id=? AND"
            + " status=2",
        id);
  }

  public void insertOperationLog(
      long userId, String type, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,target_id,description,"
            + "ip_address) VALUES(?,?,?,?,?,?)",
        userId,
        type,
        "notification_outbox",
        id,
        description,
        ipAddress);
  }
}
