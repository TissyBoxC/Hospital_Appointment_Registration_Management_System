package io.github.tissyboxc.harmsys.operation;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operation-logs")
/** 管理员分页查询系统操作日志的接口。 */
public class OperationLogController {
  private final JdbcTemplate jdbc;

  public OperationLogController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   *
   * @param user_id 用户ID
   * @param operation_type 操作类型
   * @param target_type 目标类型
   * @param target_id 目标数据ID
   * @param start_time 开始操作时间
   * @param end_time 结束时间
   * @param limit 最大数量
   */
  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Long user_id,
      @RequestParam(required = false) String operation_type,
      @RequestParam(required = false) String target_type,
      @RequestParam(required = false) Long target_id,
      @RequestParam(required = false) String start_time,
      @RequestParam(required = false) String end_time,
      @RequestParam(defaultValue = "100") int limit,
      HttpServletRequest request) {
    checkAdmin(request);
    StringBuilder sql = new StringBuilder("SELECT * FROM operation_log WHERE 1=1");
    List<Object> args = new ArrayList<>();
    //按用户筛选
    if (user_id != null) {
      sql.append(" AND user_id=?");
      args.add(user_id);
    }
    //按操作类型筛选
    if (operation_type != null && !operation_type.isBlank()) {
      sql.append(" AND operation_type=?");
      args.add(operation_type.trim());
    }
    //按目标类型筛选
    if (target_type != null && !target_type.isBlank()) {
      sql.append(" AND target_type=?");
      args.add(target_type.trim());
    }
    //按目标数据ID筛选
    if (target_id != null) {
      sql.append(" AND target_id=?");
      args.add(target_id);
    }
    //按时间筛选
    if (start_time != null && !start_time.isBlank()) {
      sql.append(" AND created_at>=?");
      args.add(start_time);
    }
    if (end_time != null && !end_time.isBlank()) {
      sql.append(" AND created_at<=?");
      args.add(end_time);
    }
    //限量
    sql.append(" ORDER BY id DESC LIMIT ").append(Math.max(1, Math.min(limit, 500)));
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  /**
   * 按ID查询单条日志
   */
  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Object> result =
        jdbc.query("SELECT * FROM operation_log WHERE id=?", rs -> rs.next() ? row(rs) : null, id);
    if (result == null) throw new UserRegistrationException(404, "操作日志不存在");
    return result;
  }

  /**
   * 分页查询
   */
  @GetMapping("/paged")
  public Map<String, Object> paged(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      HttpServletRequest request) {
    checkAdmin(request);
    int size = Math.max(1, Math.min(page_size, 100)),
        pg = Math.max(1, page),
        offset = (pg - 1) * size;
    long total = jdbc.queryForObject("SELECT COUNT(*) FROM operation_log", Long.class);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("page", pg);
    m.put("page_size", size);
    m.put("total", total);
    m.put(
        "items",
        jdbc.queryForList(
            "SELECT * FROM operation_log ORDER BY id DESC LIMIT ? OFFSET ?", size, offset));
    return m;
  }

  /**
   * 验证是否是管理员
   */
  private void checkAdmin(HttpServletRequest request) {
    var u = SessionAuth.require(request);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询操作日志");
  }

  /**
   * 将原数据库查询信息格式化
   */
  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }
}
