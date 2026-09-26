package io.github.tissyboxc.harmsys.patient.repository;

import io.github.tissyboxc.harmsys.patient.dto.PatientProfileUpdateRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 患者资料持久化访问。 */
@Repository
public class PatientRepository {
  private final JdbcTemplate jdbc;

  public PatientRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> findProfile(long patientId) {
    return jdbc.queryForMap(
        "SELECT id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,"
            + "emergency_phone FROM patient WHERE id=? AND deleted=0",
        patientId);
  }

  public int updateProfile(long patientId, PatientProfileUpdateRequest body) {
    return jdbc.update(
        "UPDATE patient SET real_name=?,phone=?,address=?,emergency_contact=?,emergency_phone=?"
            + " WHERE id=? AND deleted=0",
        body.real_name(),
        body.phone(),
        body.address(),
        body.emergency_contact(),
        body.emergency_phone(),
        patientId);
  }

  public void insertOperationLog(
      long userId, long patientId, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,target_id,description,"
            + "ip_address) VALUES(?,?,'patient',?,?,?)",
        userId,
        "UPDATE_PATIENT_PROFILE",
        patientId,
        description,
        ipAddress);
  }
}
