package io.github.tissyboxc.harmsys.operationlog.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 操作日志持久化访问。 */
@Repository
public class OperationLogRepository {
  private final JdbcTemplate jdbc;

  public OperationLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(
      Long userId,
      String operationType,
      String targetType,
      Long targetId,
      String startTime,
      String endTime,
      int limit) {
    StringBuilder sql = new StringBuilder("SELECT * FROM operation_log WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (userId != null) {
      sql.append(" AND user_id=?");
      args.add(userId);
    }
    if (operationType != null && !operationType.isBlank()) {
      sql.append(" AND operation_type=?");
      args.add(operationType.trim());
    }
    if (targetType != null && !targetType.isBlank()) {
      sql.append(" AND target_type=?");
      args.add(targetType.trim());
    }
    if (targetId != null) {
      sql.append(" AND target_id=?");
      args.add(targetId);
    }
    if (startTime != null && !startTime.isBlank()) {
      sql.append(" AND created_at>=?");
      args.add(startTime);
    }
    if (endTime != null && !endTime.isBlank()) {
      sql.append(" AND created_at<=?");
      args.add(endTime);
    }
    sql.append(" ORDER BY id DESC LIMIT ").append(limit);
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  public Map<String, Object> findById(long id) {
    return jdbc.query(
        "SELECT * FROM operation_log WHERE id=?", rs -> rs.next() ? row(rs) : null, id);
  }

  public long count() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM operation_log", Long.class);
  }

  public List<Map<String, Object>> page(int size, int offset) {
    return jdbc.queryForList(
        "SELECT * FROM operation_log ORDER BY id DESC LIMIT ? OFFSET ?", size, offset);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
