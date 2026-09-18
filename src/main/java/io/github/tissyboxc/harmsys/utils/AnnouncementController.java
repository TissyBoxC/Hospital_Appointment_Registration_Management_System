package io.github.tissyboxc.harmsys.utils;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** 公告发布、撤回和公共读取。 */
@RestController
/** 公告的查询和维护接口。 */
public class AnnouncementController {
  private final JdbcTemplate jdbc;

  public AnnouncementController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 获取已发布公告
   * @return 公告信息
   */
  @GetMapping("/api/public/announcements")
  public List<Map<String, Object>> publicList() {
    return jdbc.queryForList(
        "SELECT id,title,content,published_at FROM system_announcement WHERE status=1 ORDER BY"
            + " published_at DESC,id DESC");
  }

  /**
   * 根据公告ID返回公告信息
   * @param id 公告ID
   */
  @GetMapping("/api/public/announcements/{id}")
  public Map<String, Object> publicGet(@PathVariable long id) {
    Map<String, Object> m =
        one(
            "SELECT id,title,content,published_at FROM system_announcement WHERE id=? AND status=1",
            id);
    if (m == null) throw new UserRegistrationException(404, "公告不存在");
    return m;
  }

  /**
   * 管理员查询所有公告,包含0，1，2状态
   */
  @GetMapping("/api/admin/announcements")
  public List<Map<String, Object>> adminList(HttpServletRequest r) {
    admin(r);
    return jdbc.queryForList("SELECT * FROM system_announcement ORDER BY id DESC");
  }

  /**
   * 管理员发布公告
   * @param b 下游请求体,包含公告信息
   */
  @PostMapping("/api/admin/announcements")
  public Map<String, Object> create(@RequestBody Map<String, Object> b, HttpServletRequest r) {
    AuthenticatedUser u = admin(r);
    //解析公告信息
    String title = required(b, "title"), content = required(b, "content");
    jdbc.update(
        "INSERT INTO system_announcement(title,content,status,publisher_user_id) VALUES(?,?,0,?)",
        title,
        content,
        u.user_id());
    long id =
        jdbc.queryForObject(
            "SELECT id FROM system_announcement WHERE publisher_user_id=? ORDER BY id DESC LIMIT 1",
            Long.class,
            u.user_id());
    return one("SELECT * FROM system_announcement WHERE id=?", id);
  }

  /**
   * 管理员修改公告信息
   * @param id 公告ID
   * @param b 请求体
   */
  @PutMapping("/api/admin/announcements/{id}")
  public Map<String, Object> update(
      @PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest r) {
    admin(r);
    if (jdbc.update(
            "UPDATE system_announcement SET title=?,content=? WHERE id=? AND status=0",
            required(b, "title"),
            required(b, "content"),
            id)
        != 1) throw new UserRegistrationException(409, "公告不存在或已发布");
    return one("SELECT * FROM system_announcement WHERE id=?", id);
  }

  /**
   * 管理员发布公告
   * @param id 公告ID
   */
  @PostMapping("/api/admin/announcements/{id}/publish")
  public Map<String, Object> publish(@PathVariable long id, HttpServletRequest r) {
    admin(r);
    if (jdbc.update(
            "UPDATE system_announcement SET status=1,published_at=CURRENT_TIMESTAMP WHERE id=? AND"
                + " status=0",
            id)
        != 1) throw new UserRegistrationException(409, "公告当前不能发布");
    return one("SELECT * FROM system_announcement WHERE id=?", id);
  }

  /**
   * 管理员撤回公告
   * @param id 公告ID
   */
  @PostMapping("/api/admin/announcements/{id}/retract")
  public Map<String, Object> retract(@PathVariable long id, HttpServletRequest r) {
    admin(r);
    if (jdbc.update("UPDATE system_announcement SET status=2 WHERE id=? AND status=1", id) != 1)
      throw new UserRegistrationException(409, "公告当前不能撤回");
    return one("SELECT * FROM system_announcement WHERE id=?", id);
  }

  /**
   * 验证管理员身份
   */
  private AuthenticatedUser admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以管理公告");
    return u;
  }

  /**
   * 解析请求体
   * @param b 下游请求体
   * @param k 待解析字段
   * @return
   */
  private String required(Map<String, Object> b, String k) {
    Object v = b.get(k);
    if (v == null || String.valueOf(v).isBlank())
      throw new UserRegistrationException(422, k + "不能为空");
    return String.valueOf(v).trim();
  }

  private Map<String, Object> one(String sql, Object... a) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, a);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }
}
