package io.github.tissyboxc.harmsys.registration;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 挂号员工作台：患者检索、代挂号、现场挂号、队列操作和退号。 */
@RestController
@RequestMapping("/api/registration")
/** 挂号员患者检索、退款、叫号和爽约处理接口。 */
public class RegistrationOperationsController {
  private final JdbcTemplate jdbc;

  public RegistrationOperationsController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/patients")
  public List<Map<String, Object>> patients(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    operator(request);
    String k = keyword == null ? "" : keyword.trim();
    String like = "%" + k + "%";
    return jdbc.queryForList(
        "SELECT"
            + " id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone"
            + " FROM patient WHERE deleted=0 AND (real_name LIKE ? OR phone LIKE ? OR id_card LIKE"
            + " ?) ORDER BY id DESC LIMIT 100",
        like,
        like,
        like);
  }

  @GetMapping("/patients/{id}")
  public Map<String, Object> patient(@PathVariable long id, HttpServletRequest request) {
    operator(request);
    Map<String, Object> p =
        one(
            "SELECT"
                + " id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone"
                + " FROM patient WHERE id=? AND deleted=0",
            id);
    if (p == null) throw new UserRegistrationException(404, "患者不存在");
    return p;
  }

  @PostMapping("/appointments")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser op = operator(request);
    long patientId = number(body, "patient_id"), scheduleId = number(body, "schedule_id");
    Long slotId = body.get("slot_id") == null ? null : number(body, "slot_id");
    Map<String, Object> p = one("SELECT id FROM patient WHERE id=? AND deleted=0", patientId);
    Map<String, Object> s = one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", scheduleId);
    if (p == null) throw new UserRegistrationException(404, "患者不存在");
    if (s == null || ((Number) s.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "排班不可预约");
    if (((Number) s.get("booked_count")).intValue() >= ((Number) s.get("total_count")).intValue())
      throw new UserRegistrationException(409, "号源已满");
    if (slotId != null
        && jdbc.update(
                "UPDATE schedule_slot SET status=1 WHERE id=? AND schedule_id=? AND status=0",
                slotId,
                scheduleId)
            != 1) throw new UserRegistrationException(409, "时间段不可用");
    int queue =
        Optional.ofNullable(
                jdbc.queryForObject(
                    "SELECT COALESCE(MAX(queue_no),0)+1 FROM appointment WHERE schedule_id=? FOR"
                        + " UPDATE",
                    Integer.class,
                    scheduleId))
            .orElse(1);
    String no =
        "A"
            + System.currentTimeMillis()
            + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase(Locale.ROOT);
    jdbc.update(
        "INSERT INTO"
            + " appointment(appointment_no,patient_id,doctor_id,department_id,schedule_id,slot_id,appointment_date,period,queue_no,fee,status,remark)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,2,?)",
        no,
        patientId,
        s.get("doctor_id"),
        s.get("department_id"),
        scheduleId,
        slotId,
        s.get("schedule_date"),
        s.get("period"),
        queue,
        s.get("fee"),
        body.get("remark"));
    long id =
        jdbc.queryForObject("SELECT id FROM appointment WHERE appointment_no=?", Long.class, no);
    jdbc.update("UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=?", scheduleId);
    String payNo =
        "PAY"
            + System.currentTimeMillis()
            + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?, ?,1,2,?,CURRENT_TIMESTAMP)",
        payNo,
        id,
        patientId,
        s.get("fee"),
        "MOCK-" + payNo);
    log(op.user_id(), "REGISTRATION_CREATE_APPOINTMENT", id, "挂号员代患者挂号", request);
    return one(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id"
            + " JOIN department dp ON dp.id=a.department_id WHERE a.id=?",
        id);
  }

  @PostMapping("/appointments/{id}/refund")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void refund(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser op = operator(request);
    Map<String, Object> a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    if (jdbc.update(
            "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE"
                + " appointment_id=? AND status=2",
            id)
        != 1) throw new UserRegistrationException(409, "没有可退款的支付记录");
    jdbc.update(
        "UPDATE appointment SET status=9,cancel_reason='挂号员退款' WHERE id=? AND status IN (1,2,3,4)",
        id);
    if (a.get("slot_id") != null)
      jdbc.update(
          "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1",
          ((Number) a.get("slot_id")).longValue());
    jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        ((Number) a.get("schedule_id")).longValue());
    log(op.user_id(), "REGISTRATION_REFUND_APPOINTMENT", id, "挂号员退号并退款", request);
  }

  @PostMapping("/queue/{id}/call-next")
  public Map<String, Object> callNext(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser op = operator(request);
    if (jdbc.update("UPDATE appointment SET status=4 WHERE id=? AND status=3", id) != 1)
      throw new UserRegistrationException(409, "该预约不在候诊队列");
    jdbc.update(
        "UPDATE medical_visit SET"
            + " status=2,visit_start_at=COALESCE(visit_start_at,CURRENT_TIMESTAMP) WHERE"
            + " appointment_id=?",
        id);
    log(op.user_id(), "REGISTRATION_CALL_NEXT", id, "挂号员呼叫下一位患者", request);
    return one("SELECT * FROM appointment WHERE id=?", id);
  }

  @PostMapping("/queue/{id}/mark-no-show")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void noShow(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser op = operator(request);
    if (jdbc.update(
            "UPDATE appointment SET status=8,cancel_reason='患者过号' WHERE id=? AND status=3", id)
        != 1) throw new UserRegistrationException(409, "预约不在候诊队列");
    log(op.user_id(), "REGISTRATION_MARK_NO_SHOW", id, "标记患者过号", request);
  }

  @PostMapping("/queue/{id}/requeue")
  public Map<String, Object> requeue(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser op = operator(request);
    if (jdbc.update("UPDATE appointment SET status=3 WHERE id=? AND status=8", id) != 1)
      throw new UserRegistrationException(409, "该预约不能重新排队");
    log(op.user_id(), "REGISTRATION_REQUEUE", id, "患者重新排队", request);
    return one("SELECT * FROM appointment WHERE id=?", id);
  }

  private AuthenticatedUser operator(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream()
        .noneMatch(x -> x.equalsIgnoreCase("REGISTRATION") || x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "没有挂号员权限");
    return u;
  }

  private long number(Map<String, Object> b, String k) {
    if (b.get(k) == null) throw new UserRegistrationException(422, k + "不能为空");
    return Long.parseLong(String.valueOf(b.get(k)));
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

  private void log(long uid, String type, long id, String desc, HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        "appointment",
        id,
        desc,
        r.getRemoteAddr());
  }
}
