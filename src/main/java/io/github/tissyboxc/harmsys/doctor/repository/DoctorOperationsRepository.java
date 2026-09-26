package io.github.tissyboxc.harmsys.doctor.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 医生工作台患者与就诊历史数据访问。 */
@Repository
public class DoctorOperationsRepository {
  private final JdbcTemplate jdbc;

  public DoctorOperationsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> findPatients(long doctorId, String keyword) {
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    return jdbc.queryForList(
        "SELECT DISTINCT p.id,p.user_id,p.real_name,p.gender,p.birthday,p.phone,p.address FROM"
            + " patient p JOIN appointment a ON a.patient_id=p.id WHERE a.doctor_id=? AND"
            + " p.deleted=0 AND (p.real_name LIKE ? OR p.phone LIKE ?) ORDER BY p.real_name",
        doctorId,
        like,
        like);
  }

  public Map<String, Object> findPatient(long patientId, long doctorId) {
    return one(
        "SELECT DISTINCT"
            + " p.id,p.user_id,p.real_name,p.id_card,p.gender,p.birthday,p.phone,p.address,p.emergency_contact,p.emergency_phone"
            + " FROM patient p JOIN appointment a ON a.patient_id=p.id WHERE p.id=? AND"
            + " a.doctor_id=? AND p.deleted=0",
        patientId,
        doctorId);
  }

  public List<Map<String, Object>> appointments(long patientId, long doctorId) {
    return jdbc.queryForList(
        "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN"
            + " doctor d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
            + " a.patient_id=? AND a.doctor_id=? ORDER BY a.appointment_date DESC,a.id DESC",
        patientId,
        doctorId);
  }

  public List<Map<String, Object>> visits(long patientId, long doctorId) {
    return jdbc.queryForList(
        "SELECT * FROM medical_visit WHERE patient_id=? AND doctor_id=? ORDER BY created_at"
            + " DESC",
        patientId,
        doctorId);
  }

  public List<Map<String, Object>> diagnoses(long patientId, long doctorId) {
    return jdbc.queryForList(
        "SELECT dr.* FROM diagnosis_record dr JOIN medical_visit v ON v.id=dr.visit_id WHERE"
            + " v.patient_id=? AND v.doctor_id=? ORDER BY dr.created_at DESC",
        patientId,
        doctorId);
  }

  public List<Map<String, Object>> prescriptions(long patientId, long doctorId) {
    return jdbc.queryForList(
        "SELECT p.* FROM prescription p JOIN medical_visit v ON v.id=p.visit_id WHERE"
            + " v.patient_id=? AND v.doctor_id=? ORDER BY p.created_at DESC",
        patientId,
        doctorId);
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
