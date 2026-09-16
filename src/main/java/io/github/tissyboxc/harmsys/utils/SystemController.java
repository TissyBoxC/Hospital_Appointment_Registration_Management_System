package io.github.tissyboxc.harmsys.utils;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** 健康检查、通知和系统参数接口。 */
@RestController
@RequestMapping("/api/system")
/** 提供公开系统元数据和健康状态接口。 */
public class SystemController {
  private final JdbcTemplate jdbc;

  public SystemController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("status", "UP");
    m.put("database", jdbc.queryForObject("SELECT 1", Integer.class));
    m.put("time", java.time.OffsetDateTime.now());
    return m;
  }

  @GetMapping("/metrics")
  public Map<String, Object> metrics(HttpServletRequest request) {
    admin(request);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(
        "users", jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE deleted=0", Long.class));
    m.put(
        "patients",
        jdbc.queryForObject("SELECT COUNT(*) FROM patient WHERE deleted=0", Long.class));
    m.put(
        "doctors", jdbc.queryForObject("SELECT COUNT(*) FROM doctor WHERE deleted=0", Long.class));
    m.put(
        "active_schedules",
        jdbc.queryForObject("SELECT COUNT(*) FROM doctor_schedule WHERE status=1", Long.class));
    m.put(
        "active_appointments",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointment WHERE status IN (1,2,3,4)", Long.class));
    m.put(
        "pending_outbox",
        jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE status=0", Long.class));
    m.put(
        "active_sessions",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM active_session WHERE expires_at>CURRENT_TIMESTAMP", Long.class));
    return m;
  }

  @GetMapping("/notifications")
  public List<Map<String, Object>> notifications(HttpServletRequest request) {
    long uid = SessionAuth.require(request).user_id();
    return jdbc.queryForList(
        "SELECT * FROM system_notification WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
        uid);
  }

  @PostMapping("/notifications/{id}/read")
  public void read(@PathVariable long id, HttpServletRequest request) {
    long uid = SessionAuth.require(request).user_id();
    if (jdbc.update(
            "UPDATE system_notification SET read_status=1,read_at=CURRENT_TIMESTAMP WHERE id=? AND"
                + " user_id=?",
            id,
            uid)
        != 1) throw new io.github.tissyboxc.harmsys.users.UserRegistrationException(404, "通知不存在");
  }

  @GetMapping("/config")
  public List<Map<String, Object>> configs(HttpServletRequest request) {
    admin(request);
    return jdbc.queryForList("SELECT * FROM system_config ORDER BY config_key");
  }

  @PutMapping("/config/{key}")
  public Map<String, Object> updateConfig(
      @PathVariable String key, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    admin(request);
    Object value = body.get("config_value");
    if (value == null)
      throw new io.github.tissyboxc.harmsys.users.UserRegistrationException(
          422, "config_value不能为空");
    jdbc.update(
        "INSERT INTO system_config(config_key,config_value,description) VALUES(?,?,?) ON DUPLICATE"
            + " KEY UPDATE config_value=VALUES(config_value),description=VALUES(description)",
        key,
        String.valueOf(value),
        body.get("description"));
    return jdbc.queryForMap("SELECT * FROM system_config WHERE config_key=?", key);
  }

  private void admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以管理系统参数");
  }
}
