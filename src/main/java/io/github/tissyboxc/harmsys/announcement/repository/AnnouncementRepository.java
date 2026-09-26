package io.github.tissyboxc.harmsys.announcement.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 公告持久化访问。 */
@Repository
public class AnnouncementRepository {
  private final JdbcTemplate jdbc;

  public AnnouncementRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> findPublished() {
    return jdbc.queryForList(
        "SELECT id,title,content,published_at FROM system_announcement WHERE status=1 ORDER BY"
            + " published_at DESC,id DESC");
  }

  public Map<String, Object> findPublishedById(long id) {
    return one(
        "SELECT id,title,content,published_at FROM system_announcement WHERE id=? AND status=1",
        id);
  }

  public List<Map<String, Object>> findAll() {
    return jdbc.queryForList("SELECT * FROM system_announcement ORDER BY id DESC");
  }

  public long insert(String title, String content, long publisherUserId) {
    jdbc.update(
        "INSERT INTO system_announcement(title,content,status,publisher_user_id) VALUES(?,?,0,?)",
        title,
        content,
        publisherUserId);
    return jdbc.queryForObject(
        "SELECT id FROM system_announcement WHERE publisher_user_id=? ORDER BY id DESC LIMIT 1",
        Long.class,
        publisherUserId);
  }

  public int updateDraft(long id, String title, String content) {
    return jdbc.update(
        "UPDATE system_announcement SET title=?,content=? WHERE id=? AND status=0",
        title,
        content,
        id);
  }

  public int publish(long id) {
    return jdbc.update(
        "UPDATE system_announcement SET status=1,published_at=CURRENT_TIMESTAMP WHERE id=? AND"
            + " status=0",
        id);
  }

  public int retract(long id) {
    return jdbc.update(
        "UPDATE system_announcement SET status=2 WHERE id=? AND status=1", id);
  }

  public Map<String, Object> findById(long id) {
    return one("SELECT * FROM system_announcement WHERE id=?", id);
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
