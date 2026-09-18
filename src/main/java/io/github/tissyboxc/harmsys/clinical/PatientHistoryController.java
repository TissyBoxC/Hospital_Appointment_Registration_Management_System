package io.github.tissyboxc.harmsys.clinical;

import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
/** 患者端完整医疗历史查询。 */
public class PatientHistoryController {
  private final JdbcTemplate jdbc;

  public PatientHistoryController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 查看患者自己的处方列表
   */
  @GetMapping("/prescriptions")
  public List<Map<String, Object>> prescriptions(HttpServletRequest request) {
    long p = patient(request).patient_id();
    return jdbc.queryForList(
        "SELECT rx.*,v.visit_no,d.real_name doctor_name FROM prescription rx JOIN medical_visit v"
            + " ON v.id=rx.visit_id JOIN doctor d ON d.id=rx.doctor_id WHERE v.patient_id=? ORDER"
            + " BY rx.created_at DESC",
        p);
  }

  /**
   * 查看某张处方的药品明细
   */
  @GetMapping("/prescriptions/{id}/items")
  public List<Map<String, Object>> items(@PathVariable long id, HttpServletRequest request) {
    long p = patient(request).patient_id();
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM prescription rx JOIN medical_visit v ON v.id=rx.visit_id WHERE"
                + " rx.id=? AND v.patient_id=?",
            Long.class,
            id,
            p)
        == 0) throw new UserRegistrationException(404, "处方不存在");
    return jdbc.queryForList(
        "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", id);
  }

  /**
   * 查看自己的诊断记录或指定某张诊断
   */
  @GetMapping("/diagnoses")
  public List<Map<String, Object>> diagnoses(
      @RequestParam(required = false) Long visit_id, HttpServletRequest request) {
    long p = patient(request).patient_id();
    if (visit_id == null)
      return jdbc.queryForList(
          "SELECT dr.*,v.visit_no,d.real_name doctor_name FROM diagnosis_record dr JOIN"
              + " medical_visit v ON v.id=dr.visit_id JOIN doctor d ON d.id=v.doctor_id WHERE"
              + " v.patient_id=? ORDER BY dr.created_at DESC",
          p);
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM medical_visit WHERE id=? AND patient_id=?",
            Long.class,
            visit_id,
            p)
        == 0) throw new UserRegistrationException(404, "就诊记录不存在");
    return jdbc.queryForList(
        "SELECT * FROM diagnosis_record WHERE visit_id=? ORDER BY id", visit_id);
  }

  /**
   * 验证当前登陆账户身份
   */
  private AuthenticatedUser patient(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return u;
  }
}
