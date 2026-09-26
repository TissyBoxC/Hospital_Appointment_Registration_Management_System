package io.github.tissyboxc.harmsys.system.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 系统配置、通知与监控数据访问。 */
@Repository
public class SystemRepository {
  private final JdbcTemplate jdbc;

  public SystemRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Integer databaseCheck() {
    return jdbc.queryForObject("SELECT 1", Integer.class);
  }

  public long count(String table, String condition) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM " + table + (condition == null ? "" : " WHERE " + condition),
        Long.class);
  }

  public List<Map<String, Object>> notifications(long userId) {
    return jdbc.queryForList(
        "SELECT * FROM system_notification WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
        userId);
  }

  public int markNotificationRead(long id, long userId) {
    return jdbc.update(
        "UPDATE system_notification SET read_status=1,read_at=CURRENT_TIMESTAMP WHERE id=? AND"
            + " user_id=?",
        id,
        userId);
  }

  public List<Map<String, Object>> configs() {
    return jdbc.queryForList("SELECT * FROM system_config ORDER BY config_key");
  }

  public void upsertConfig(String key, String value, Object description) {
    jdbc.update(
        "INSERT INTO system_config(config_key,config_value,description) VALUES(?,?,?) ON DUPLICATE"
            + " KEY UPDATE config_value=VALUES(config_value),description=VALUES(description)",
        key,
        value,
        description);
  }

  public Map<String, Object> findConfig(String key) {
    return jdbc.queryForMap("SELECT * FROM system_config WHERE config_key=?", key);
  }
}
