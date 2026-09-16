package io.github.tissyboxc.harmsys.doctor;

import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
/** 医生患者历史查询及时间段完整维护。 */
public class DoctorOperationsController {
  private final JdbcTemplate jdbc;

  public DoctorOperationsController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/patients")
  public List<Map<String, Object>> patients(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    long doctor = doctor(request).doctor_id();
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    return jdbc.queryForList(
        "SELECT DISTINCT p.id,p.user_id,p.real_name,p.gender,p.birthday,p.phone,p.address FROM"
            + " patient p JOIN appointment a ON a.patient_id=p.id WHERE a.doctor_id=? AND"
            + " p.deleted=0 AND (p.real_name LIKE ? OR p.phone LIKE ?) ORDER BY p.real_name",
        doctor,
        like,
        like);
  }

  @GetMapping("/patients/{id}")
  public Map<String, Object> patient(@PathVariable long id, HttpServletRequest request) {
    long doctor = doctor(request).doctor_id();
    Map<String, Object> p =
        one(
            "SELECT DISTINCT"
                + " p.id,p.user_id,p.real_name,p.id_card,p.gender,p.birthday,p.phone,p.address,p.emergency_contact,p.emergency_phone"
                + " FROM patient p JOIN appointment a ON a.patient_id=p.id WHERE p.id=? AND"
                + " a.doctor_id=? AND p.deleted=0",
            id,
            doctor);
    if (p == null) throw new UserRegistrationException(404, "患者不存在或未挂过当前医生的号");
    return p;
  }

  @GetMapping("/patients/{id}/history")
  public Map<String, Object> history(@PathVariable long id, HttpServletRequest request) {
    long doctor = doctor(request).doctor_id();
    patient(id, request);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "appointments",
        jdbc.queryForList(
            "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN"
                + " doctor d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
                + " a.patient_id=? AND a.doctor_id=? ORDER BY a.appointment_date DESC,a.id DESC",
            id,
            doctor));
    result.put(
        "visits",
        jdbc.queryForList(
            "SELECT * FROM medical_visit WHERE patient_id=? AND doctor_id=? ORDER BY created_at"
                + " DESC",
            id,
            doctor));
    result.put(
        "diagnoses",
        jdbc.queryForList(
            "SELECT dr.* FROM diagnosis_record dr JOIN medical_visit v ON v.id=dr.visit_id WHERE"
                + " v.patient_id=? AND v.doctor_id=? ORDER BY dr.created_at DESC",
            id,
            doctor));
    result.put(
        "prescriptions",
        jdbc.queryForList(
            "SELECT p.* FROM prescription p JOIN medical_visit v ON v.id=p.visit_id WHERE"
                + " v.patient_id=? AND v.doctor_id=? ORDER BY p.created_at DESC",
            id,
            doctor));
    return result;
  }

  @PutMapping("/slots/{slotId}")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateSlot(
      @PathVariable long slotId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser d = doctor(request);
    Map<String, Object> slot =
        one(
            "SELECT s.*,ds.doctor_id,ds.start_time schedule_start,ds.end_time schedule_end FROM"
                + " schedule_slot s JOIN doctor_schedule ds ON ds.id=s.schedule_id WHERE s.id=?",
            slotId);
    if (slot == null || ((Number) slot.get("doctor_id")).longValue() != d.doctor_id())
      throw new UserRegistrationException(404, "时间段不存在或不属于当前医生");
    if (((Number) slot.get("status")).intValue() == 1)
      throw new UserRegistrationException(409, "已预约时间段不能修改");
    LocalTime start = LocalTime.parse(String.valueOf(body.get("start_time"))),
        end = LocalTime.parse(String.valueOf(body.get("end_time")));
    int no = Integer.parseInt(String.valueOf(body.get("slot_no")));
    LocalTime ss = (LocalTime) slot.get("schedule_start"),
        se = (LocalTime) slot.get("schedule_end");
    if (!start.isBefore(end) || start.isBefore(ss) || end.isAfter(se))
      throw new UserRegistrationException(422, "时间段不在排班范围内");
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM schedule_slot WHERE schedule_id=? AND id<>? AND (slot_no=? OR"
                + " (start_time < ? AND end_time > ?))",
            Long.class,
            slot.get("schedule_id"),
            slotId,
            no,
            end,
            start)
        > 0) throw new UserRegistrationException(409, "时间段序号或时间范围冲突");
    jdbc.update(
        "UPDATE schedule_slot SET slot_no=?,start_time=?,end_time=? WHERE id=?",
        no,
        start,
        end,
        slotId);
    log(d.user_id(), "UPDATE_SLOT", "schedule_slot", slotId, "医生修改时间段", request);
    return one("SELECT * FROM schedule_slot WHERE id=?", slotId);
  }

  @DeleteMapping("/slots/{slotId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void deleteSlot(@PathVariable long slotId, HttpServletRequest request) {
    AuthenticatedUser d = doctor(request);
    Map<String, Object> slot =
        one(
            "SELECT s.*,ds.doctor_id FROM schedule_slot s JOIN doctor_schedule ds ON"
                + " ds.id=s.schedule_id WHERE s.id=?",
            slotId);
    if (slot == null || ((Number) slot.get("doctor_id")).longValue() != d.doctor_id())
      throw new UserRegistrationException(404, "时间段不存在或不属于当前医生");
    if (((Number) slot.get("status")).intValue() != 0
        || jdbc.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE slot_id=? AND status IN (1,2,3,4)",
                Long.class,
                slotId)
            > 0) throw new UserRegistrationException(409, "已有预约的时间段不能删除");
    jdbc.update("DELETE FROM schedule_slot WHERE id=?", slotId);
    log(d.user_id(), "DELETE_SLOT", "schedule_slot", slotId, "医生删除时间段", request);
  }

  private AuthenticatedUser doctor(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("DOCTOR")))
      throw new UserRegistrationException(403, "当前账号不是医生");
    return u;
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

  private void log(
      long uid, String type, String target, long id, String desc, HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        target,
        id,
        desc,
        r.getRemoteAddr());
  }
}
