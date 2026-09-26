package io.github.tissyboxc.harmsys.clinical.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 患者医疗历史查询。 */
@Repository
public class PatientHistoryRepository {
  private final JdbcTemplate jdbc;

  public PatientHistoryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> prescriptions(long patientId) {
    return jdbc.queryForList(
        "SELECT rx.*,v.visit_no,d.real_name doctor_name FROM prescription rx JOIN medical_visit v"
            + " ON v.id=rx.visit_id JOIN doctor d ON d.id=rx.doctor_id WHERE v.patient_id=? ORDER"
            + " BY rx.created_at DESC",
        patientId);
  }

  public boolean ownsPrescription(long prescriptionId, long patientId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM prescription rx JOIN medical_visit v ON v.id=rx.visit_id WHERE"
                + " rx.id=? AND v.patient_id=?",
            Long.class,
            prescriptionId,
            patientId)
        > 0;
  }

  public List<Map<String, Object>> prescriptionItems(long prescriptionId) {
    return jdbc.queryForList(
        "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", prescriptionId);
  }

  public List<Map<String, Object>> diagnoses(long patientId) {
    return jdbc.queryForList(
        "SELECT dr.*,v.visit_no,d.real_name doctor_name FROM diagnosis_record dr JOIN"
            + " medical_visit v ON v.id=dr.visit_id JOIN doctor d ON d.id=v.doctor_id WHERE"
            + " v.patient_id=? ORDER BY dr.created_at DESC",
        patientId);
  }

  public boolean ownsVisit(long visitId, long patientId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM medical_visit WHERE id=? AND patient_id=?",
            Long.class,
            visitId,
            patientId)
        > 0;
  }

  public List<Map<String, Object>> diagnosesByVisit(long visitId) {
    return jdbc.queryForList(
        "SELECT * FROM diagnosis_record WHERE visit_id=? ORDER BY id", visitId);
  }
}
