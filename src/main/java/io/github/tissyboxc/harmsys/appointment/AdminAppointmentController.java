package io.github.tissyboxc.harmsys.appointment;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appointments")
/** 管理员查询全量预约及分页数据。 */
public class AdminAppointmentController {
  private final JdbcTemplate jdbc;

  public AdminAppointmentController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  public List<Map<String, Object>> list(HttpServletRequest r) {
    check(r);
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name "
            + "FROM appointment a J"
            + "OIN patient p "
            + "ON p.id=a.patient_id "
            + "JOIN doctor d ON d.id=a.doctor_id "
            + "JOIN department dp "
            + "ON dp.id=a.department_id "
            + "ORDER BY a.appointment_date DESC,a.id DESC");
  }

  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest r) {
    check(r);
    int pg = Math.max(1, page), s = Math.min(Math.max(1, page_size), 100), off = (pg - 1) * s;
    String extra = status == null ? "" : " WHERE a.status=?";
    List<Map<String, Object>> items =
        status == null
            ? jdbc.queryForList(
                "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name"
                    + " department_name FROM appointment a JOIN patient p ON p.id=a.patient_id JOIN"
                    + " doctor d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id"
                    + " ORDER BY a.appointment_date DESC,a.id DESC LIMIT ? OFFSET ?",
                s,
                off)
            : jdbc.queryForList(
                "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name"
                    + " department_name FROM appointment a JOIN patient p ON p.id=a.patient_id JOIN"
                    + " doctor d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id"
                    + " WHERE a.status=? ORDER BY a.appointment_date DESC,a.id DESC LIMIT ? OFFSET"
                    + " ?",
                status,
                s,
                off);
    long total =
        status == null
            ? jdbc.queryForObject("SELECT COUNT(*) " + "FROM appointment a", Long.class)
            : jdbc.queryForObject(
                "SELECT COUNT(*) " + "FROM appointment a " + "WHERE a.status=?",
                Long.class,
                status);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("page", pg);
    m.put("page_size", s);
    m.put("total", total);
    m.put("items", items);
    return m;
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest r) {
    check(r);
    Map<String, Object> m =
        jdbc.query(
            "SELECT a.*,p.real_name patient_name,p.phone patient_phone,d.real_name"
                + " doctor_name,dp.name department_name FROM appointment a JOIN patient p ON"
                + " p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id JOIN department dp ON"
                + " dp.id=a.department_id WHERE a.id=?",
            rs -> rs.next() ? row(rs) : null,
            id);
    if (m == null) throw new UserRegistrationException(404, "预约不存在");
    return m;
  }

  private void check(HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询预约");
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }
}
