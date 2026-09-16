package io.github.tissyboxc.harmsys.payment;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** 支付记录列表、预约支付关联查询和管理员对账接口。 */
@RestController
@RequestMapping("/api")
/** 患者和挂号员查询支付记录的接口。 */
public class PaymentQueryController {
  private final JdbcTemplate jdbc;

  public PaymentQueryController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/patient/payments")
  public Map<String, Object> patientPayments(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      HttpServletRequest request) {
    AuthenticatedUser u = patient(request);
    return page(
        "SELECT * FROM payment_record WHERE patient_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?",
        "SELECT COUNT(*) FROM payment_record WHERE patient_id=?",
        u.patient_id(),
        page,
        page_size);
  }

  @GetMapping("/patient/appointments/{appointmentId}/payment")
  public Map<String, Object> appointmentPayment(
      @PathVariable long appointmentId, HttpServletRequest request) {
    AuthenticatedUser u = patient(request);
    Map<String, Object> p =
        one(
            "SELECT pr.* FROM payment_record pr JOIN appointment a ON a.id=pr.appointment_id WHERE"
                + " pr.appointment_id=? AND a.patient_id=? ORDER BY pr.id DESC LIMIT 1",
            appointmentId,
            u.patient_id());
    if (p == null) throw new UserRegistrationException(404, "该预约没有支付记录");
    return p;
  }

  @GetMapping("/admin/payments")
  public Map<String, Object> adminPayments(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    admin(request);
    int offset = Math.max(0, page - 1) * Math.min(Math.max(page_size, 1), 100);
    String where = status == null ? "" : " WHERE pr.status=" + status;
    List<Map<String, Object>> items =
        jdbc.queryForList(
            "SELECT pr.*,p.real_name patient_name,a.appointment_no FROM payment_record pr JOIN"
                + " patient p ON p.id=pr.patient_id JOIN appointment a ON a.id=pr.appointment_id"
                + where
                + " ORDER BY pr.created_at DESC LIMIT ? OFFSET ?",
            Math.min(Math.max(page_size, 1), 100),
            offset);
    long total = jdbc.queryForObject("SELECT COUNT(*) FROM payment_record pr" + where, Long.class);
    return result(page, page_size, total, items);
  }

  private Map<String, Object> page(String sql, String countSql, long patient, int page, int size) {
    int s = Math.min(Math.max(size, 1), 100), pg = Math.max(page, 1), offset = (pg - 1) * s;
    List<Map<String, Object>> items = jdbc.queryForList(sql, patient, s, offset);
    long total = jdbc.queryForObject(countSql, Long.class, patient);
    return result(pg, s, total, items);
  }

  private Map<String, Object> result(int page, int size, long total, Object items) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("page", page);
    m.put("page_size", size);
    m.put("total", total);
    m.put("items", items);
    return m;
  }

  private AuthenticatedUser patient(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return u;
  }

  private void admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询支付记录");
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
