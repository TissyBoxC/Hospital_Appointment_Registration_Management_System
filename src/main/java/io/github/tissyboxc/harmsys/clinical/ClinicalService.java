package io.github.tissyboxc.harmsys.clinical;

import io.github.tissyboxc.harmsys.clinical.dto.*;
import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 负责就诊、诊断和处方状态流转的业务服务。 */
public class ClinicalService {
  private final JdbcTemplate jdbc;

  public ClinicalService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> doctorAppointments(HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone,d.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department d ON"
            + " d.id=a.department_id WHERE a.doctor_id=? ORDER BY"
            + " a.appointment_date,a.queue_no,a.id",
        doctor);
  }

  public Map<String, Object> doctorAppointmentsPage(
      HttpServletRequest r, int page, int size, Integer status) {
    long doctor = requireDoctor(r).doctor_id();
    int pg = Math.max(1, page), s = Math.min(Math.max(1, size), 100), offset = (pg - 1) * s;
    String extra = status == null ? "" : " AND a.status=?";
    List<Object> args = new ArrayList<>();
    args.add(doctor);
    if (status != null) args.add(status);
    args.add(s);
    args.add(offset);
    List<Map<String, Object>> items =
        jdbc.queryForList(
            "SELECT a.*,p.real_name patient_name,p.phone patient_phone,d.name department_name FROM"
                + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department d ON"
                + " d.id=a.department_id WHERE a.doctor_id=?"
                + extra
                + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?",
            args.toArray());
    List<Object> countArgs = new ArrayList<>();
    countArgs.add(doctor);
    if (status != null) countArgs.add(status);
    Long total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM appointment a WHERE a.doctor_id=?" + extra,
            Long.class,
            countArgs.toArray());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("page", pg);
    out.put("page_size", s);
    out.put("total", total);
    out.put("items", items);
    return out;
  }

  public Map<String, Object> doctorAppointment(long id, HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    Map<String, Object> m =
        one(
            "SELECT a.*,p.real_name patient_name,p.phone patient_phone,p.gender"
                + " patient_gender,p.birthday patient_birthday,d.name department_name FROM"
                + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department d ON"
                + " d.id=a.department_id WHERE a.id=? AND a.doctor_id=?",
            id,
            doctor);
    if (m == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    return m;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateDoctorAppointment(
      long appointmentId, DoctorAppointmentUpdateRequest x, HttpServletRequest r) {
    AuthenticatedUser doctor = requireDoctor(r);
    Map<String, Object> appointment =
        lock(
            "SELECT * FROM appointment WHERE id=? AND doctor_id=? FOR UPDATE",
            appointmentId,
            doctor.doctor_id());
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM department WHERE id=? AND status=1 AND deleted=0",
            Long.class,
            x.department_id())
        == 0) throw new UserRegistrationException(404, "科室不存在或已停用");
    long patientId = ((Number) appointment.get("patient_id")).longValue();
    if (jdbc.update(
            "UPDATE patient SET real_name=? WHERE id=? AND deleted=0", x.patient_name(), patientId)
        != 1) throw new UserRegistrationException(404, "患者资料不存在");
    jdbc.update(
        "UPDATE appointment SET"
            + " appointment_date=?,period=?,department_id=?,queue_no=?,status=?,remark=? WHERE"
            + " id=?",
        x.appointment_date(),
        x.period(),
        x.department_id(),
        x.queue_no(),
        x.status(),
        x.remark(),
        appointmentId);
    log(
        doctor.user_id(),
        "UPDATE_DOCTOR_APPOINTMENT",
        "appointment",
        appointmentId,
        "医生修改患者姓名及预约信息",
        r.getRemoteAddr());
    log(
        doctor.user_id(),
        "UPDATE_PATIENT_NAME",
        "patient",
        patientId,
        "医生修改预约患者姓名",
        r.getRemoteAddr());
    return doctorAppointment(appointmentId, r);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> startVisit(long appointmentId, HttpServletRequest r) {
    AuthenticatedUser doctor = requireDoctor(r);
    Map<String, Object> a =
        lock(
            "SELECT * FROM appointment WHERE id=? AND doctor_id=? FOR UPDATE",
            appointmentId,
            doctor.doctor_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    if (((Number) a.get("status")).intValue() != 3)
      throw new UserRegistrationException(409, "只有已签到预约可以开始就诊");
    Map<String, Object> v =
        one("SELECT * FROM medical_visit WHERE appointment_id=?", appointmentId);
    if (v == null) {
      String no =
          "V"
              + System.currentTimeMillis()
              + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
      jdbc.update(
          "INSERT INTO"
              + " medical_visit(appointment_id,patient_id,doctor_id,visit_no,visit_start_at,status)"
              + " VALUES(?,?,?,?,CURRENT_TIMESTAMP,2)",
          appointmentId,
          a.get("patient_id"),
          a.get("doctor_id"),
          no);
    } else {
      jdbc.update(
          "UPDATE medical_visit SET"
              + " visit_start_at=COALESCE(visit_start_at,CURRENT_TIMESTAMP),status=2 WHERE id=?",
          v.get("id"));
    }
    jdbc.update("UPDATE appointment SET status=4 WHERE id=?", appointmentId);
    log(
        doctor.user_id(),
        "START_VISIT",
        "medical_visit",
        appointmentId,
        "医生开始就诊",
        r.getRemoteAddr());
    return visitByAppointment(appointmentId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> completeVisit(long appointmentId, HttpServletRequest r) {
    AuthenticatedUser doctor = requireDoctor(r);
    Map<String, Object> a =
        lock(
            "SELECT * FROM appointment WHERE id=? AND doctor_id=? FOR UPDATE",
            appointmentId,
            doctor.doctor_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    if (((Number) a.get("status")).intValue() != 4)
      throw new UserRegistrationException(409, "只有就诊中的预约可以完成");
    Map<String, Object> v =
        one("SELECT * FROM medical_visit WHERE appointment_id=?", appointmentId);
    if (v == null) throw new UserRegistrationException(409, "就诊记录不存在");
    jdbc.update(
        "UPDATE medical_visit SET visit_end_at=CURRENT_TIMESTAMP,status=3 WHERE id=?", v.get("id"));
    jdbc.update("UPDATE appointment SET status=5 WHERE id=?", appointmentId);
    log(
        doctor.user_id(),
        "COMPLETE_VISIT",
        "medical_visit",
        ((Number) v.get("id")).longValue(),
        "医生完成就诊",
        r.getRemoteAddr());
    return visitByAppointment(appointmentId);
  }

  public List<Map<String, Object>> doctorVisits(HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    return jdbc.queryForList(
        "SELECT v.*,p.real_name patient_name,a.appointment_no FROM medical_visit v JOIN patient p"
            + " ON p.id=v.patient_id JOIN appointment a ON a.id=v.appointment_id WHERE"
            + " v.doctor_id=? ORDER BY v.created_at DESC",
        doctor);
  }

  public List<Map<String, Object>> patientVisits(HttpServletRequest r) {
    long patient = requirePatient(r).patient_id();
    return jdbc.queryForList(
        "SELECT v.*,d.real_name doctor_name,a.appointment_no FROM medical_visit v JOIN doctor d ON"
            + " d.id=v.doctor_id JOIN appointment a ON a.id=v.appointment_id WHERE v.patient_id=?"
            + " ORDER BY v.created_at DESC",
        patient);
  }

  public Map<String, Object> getVisit(long id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> v =
        one(
            "SELECT v.*,p.real_name patient_name,d.real_name doctor_name,a.appointment_no FROM"
                + " medical_visit v JOIN patient p ON p.id=v.patient_id JOIN doctor d ON"
                + " d.id=v.doctor_id JOIN appointment a ON a.id=v.appointment_id WHERE v.id=?",
            id);
    if (v == null) throw new UserRegistrationException(404, "就诊记录不存在");
    if (!isAdmin(u)
        && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
        && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该就诊记录");
    return v;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateVisit(long id, VisitUpdateRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> v = doctorVisit(id, d.doctor_id());
    if (v == null) throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    if (((Number) v.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "已完成的就诊记录不能修改");
    jdbc.update(
        "UPDATE medical_visit SET chief_complaint=?,present_illness=?,medical_advice=? WHERE id=?",
        x.chief_complaint(),
        x.present_illness(),
        x.medical_advice(),
        id);
    log(d.user_id(), "UPDATE_VISIT", "medical_visit", id, "医生修改就诊记录", r.getRemoteAddr());
    return getVisit(id, r);
  }

  public List<Map<String, Object>> diagnoses(long visitId, HttpServletRequest r) {
    doctorVisitOrPatient(visitId, r);
    return jdbc.queryForList(
        "SELECT * FROM diagnosis_record WHERE visit_id=? ORDER BY id", visitId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> createDiagnosis(
      long visitId, DiagnosisRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (doctorVisit(visitId, d.doctor_id()) == null)
      throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    KeyHolder h = new GeneratedKeyHolder();
    int n =
        jdbc.update(
            c -> {
              PreparedStatement p =
                  c.prepareStatement(
                      "INSERT INTO"
                          + " diagnosis_record(visit_id,diagnosis_name,diagnosis_code,diagnosis_type,remark)"
                          + " VALUES(?,?,?,?,?)",
                      Statement.RETURN_GENERATED_KEYS);
              p.setLong(1, visitId);
              p.setString(2, x.diagnosis_name());
              p.setString(3, x.diagnosis_code());
              p.setInt(4, x.diagnosis_type());
              p.setString(5, x.remark());
              return p;
            },
            h);
    if (n != 1 || h.getKey() == null) throw new IllegalStateException("创建诊断失败");
    log(
        d.user_id(),
        "CREATE_DIAGNOSIS",
        "diagnosis_record",
        h.getKey().longValue(),
        "医生创建诊断",
        r.getRemoteAddr());
    return one("SELECT * FROM diagnosis_record WHERE id=?", h.getKey().longValue());
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateDiagnosis(long id, DiagnosisRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> m =
        one(
            "SELECT dr.* FROM diagnosis_record dr JOIN medical_visit v ON v.id=dr.visit_id WHERE"
                + " dr.id=? AND v.doctor_id=?",
            id,
            d.doctor_id());
    if (m == null) throw new UserRegistrationException(404, "诊断不存在或不属于当前医生");
    jdbc.update(
        "UPDATE diagnosis_record SET diagnosis_name=?,diagnosis_code=?,diagnosis_type=?,remark=?"
            + " WHERE id=?",
        x.diagnosis_name(),
        x.diagnosis_code(),
        x.diagnosis_type(),
        x.remark(),
        id);
    log(d.user_id(), "UPDATE_DIAGNOSIS", "diagnosis_record", id, "医生修改诊断", r.getRemoteAddr());
    return one("SELECT * FROM diagnosis_record WHERE id=?", id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteDiagnosis(long id, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (one(
            "SELECT dr.id FROM diagnosis_record dr JOIN medical_visit v ON v.id=dr.visit_id WHERE"
                + " dr.id=? AND v.doctor_id=?",
            id,
            d.doctor_id())
        == null) throw new UserRegistrationException(404, "诊断不存在或不属于当前医生");
    jdbc.update("DELETE FROM diagnosis_record WHERE id=?", id);
    log(d.user_id(), "DELETE_DIAGNOSIS", "diagnosis_record", id, "医生删除诊断", r.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> createPrescription(PrescriptionRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (doctorVisit(x.visit_id(), d.doctor_id()) == null)
      throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    String no =
        "RX"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    KeyHolder h = new GeneratedKeyHolder();
    int n =
        jdbc.update(
            c -> {
              PreparedStatement p =
                  c.prepareStatement(
                      "INSERT INTO prescription(visit_id,prescription_no,doctor_id,status)"
                          + " VALUES(?,?,?,1)",
                      Statement.RETURN_GENERATED_KEYS);
              p.setLong(1, x.visit_id());
              p.setString(2, no);
              p.setLong(3, d.doctor_id());
              return p;
            },
            h);
    if (n != 1 || h.getKey() == null) throw new IllegalStateException("创建处方失败");
    log(
        d.user_id(),
        "CREATE_PRESCRIPTION",
        "prescription",
        h.getKey().longValue(),
        "医生创建处方",
        r.getRemoteAddr());
    return prescription(h.getKey().longValue(), r);
  }

  public Map<String, Object> prescription(long id, HttpServletRequest r) {
    Map<String, Object> p = one("SELECT * FROM prescription WHERE id=?", id);
    if (p == null) throw new UserRegistrationException(404, "处方不存在");
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> v =
        one("SELECT patient_id,doctor_id FROM medical_visit WHERE id=?", p.get("visit_id"));
    if (!isAdmin(u)
        && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
        && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该处方");
    p.put(
        "items",
        jdbc.queryForList(
            "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", id));
    return p;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updatePrescriptionStatus(long id, int status, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> p =
        one("SELECT * FROM prescription WHERE id=? AND doctor_id=?", id, d.doctor_id());
    if (p == null) throw new UserRegistrationException(404, "处方不存在或不属于当前医生");
    if (status < 1 || status > 3) throw new UserRegistrationException(422, "处方状态只能为1、2、3");
    if (((Number) p.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "已取药处方不能修改");
    jdbc.update("UPDATE prescription SET status=? WHERE id=?", status, id);
    log(
        d.user_id(),
        "UPDATE_PRESCRIPTION_STATUS",
        "prescription",
        id,
        "医生修改处方状态",
        r.getRemoteAddr());
    return prescription(id, r);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> addItem(
      long prescriptionId, PrescriptionItemRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> p =
        one("SELECT * FROM prescription WHERE id=? AND doctor_id=?", prescriptionId, d.doctor_id());
    if (p == null) throw new UserRegistrationException(404, "处方不存在或不属于当前医生");
    if (((Number) p.get("status")).intValue() != 1)
      throw new UserRegistrationException(409, "只有草稿处方可以添加明细");
    KeyHolder h = new GeneratedKeyHolder();
    int n =
        jdbc.update(
            c -> {
              PreparedStatement s =
                  c.prepareStatement(
                      "INSERT INTO"
                          + " prescription_item(prescription_id,drug_name,specification,dosage,frequency,days,quantity,remark)"
                          + " VALUES(?,?,?,?,?,?,?,?)",
                      Statement.RETURN_GENERATED_KEYS);
              s.setLong(1, prescriptionId);
              s.setString(2, x.drug_name());
              s.setString(3, x.specification());
              s.setString(4, x.dosage());
              s.setString(5, x.frequency());
              s.setInt(6, x.days());
              s.setBigDecimal(7, x.quantity());
              s.setString(8, x.remark());
              return s;
            },
            h);
    if (n != 1 || h.getKey() == null) throw new IllegalStateException("添加处方明细失败");
    log(
        d.user_id(),
        "CREATE_PRESCRIPTION_ITEM",
        "prescription_item",
        h.getKey().longValue(),
        "医生添加处方明细",
        r.getRemoteAddr());
    return one("SELECT * FROM prescription_item WHERE id=?", h.getKey().longValue());
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteItem(long id, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (one(
            "SELECT i.id FROM prescription_item i JOIN prescription p ON p.id=i.prescription_id"
                + " WHERE i.id=? AND p.doctor_id=? AND p.status=1",
            id,
            d.doctor_id())
        == null) throw new UserRegistrationException(404, "处方明细不存在或不可删除");
    jdbc.update("DELETE FROM prescription_item WHERE id=?", id);
    log(
        d.user_id(),
        "DELETE_PRESCRIPTION_ITEM",
        "prescription_item",
        id,
        "医生删除处方明细",
        r.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateItem(long id, PrescriptionItemRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (one(
            "SELECT i.id FROM prescription_item i JOIN prescription p ON p.id=i.prescription_id"
                + " WHERE i.id=? AND p.doctor_id=? AND p.status=1",
            id,
            d.doctor_id())
        == null) throw new UserRegistrationException(404, "处方明细不存在或不可修改");
    jdbc.update(
        "UPDATE prescription_item SET"
            + " drug_name=?,specification=?,dosage=?,frequency=?,days=?,quantity=?,remark=? WHERE"
            + " id=?",
        x.drug_name(),
        x.specification(),
        x.dosage(),
        x.frequency(),
        x.days(),
        x.quantity(),
        x.remark(),
        id);
    log(
        d.user_id(),
        "UPDATE_PRESCRIPTION_ITEM",
        "prescription_item",
        id,
        "医生修改处方明细",
        r.getRemoteAddr());
    return one("SELECT * FROM prescription_item WHERE id=?", id);
  }

  private Map<String, Object> visitByAppointment(long id) {
    Map<String, Object> v = one("SELECT * FROM medical_visit WHERE appointment_id=?", id);
    return v;
  }

  private Map<String, Object> doctorVisit(long id, long doctor) {
    return one("SELECT * FROM medical_visit WHERE id=? AND doctor_id=?", id, doctor);
  }

  private Map<String, Object> doctorVisitOrPatient(long id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> v = one("SELECT * FROM medical_visit WHERE id=?", id);
    if (v == null
        || (!isAdmin(u)
            && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
            && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue())))
      throw new SessionAuthenticationException(403, "无权访问该就诊记录");
    return v;
  }

  private Map<String, Object> lock(String sql, Object... a) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, a);
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

  private void log(long uid, String type, String target, long id, String desc, String ip) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        target,
        id,
        desc,
        ip);
  }

  private boolean isAdmin(AuthenticatedUser u) {
    return u.role_codes().stream().anyMatch(x -> x.equalsIgnoreCase("ADMIN"));
  }

  private AuthenticatedUser requireDoctor(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  private AuthenticatedUser requirePatient(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return u;
  }
}
