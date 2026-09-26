package io.github.tissyboxc.harmsys.admin.repository;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 管理员维护患者和医生资料的数据访问层。 */
@Repository
public class AdminProfileRepository {
  private final JdbcTemplate jdbc;

  public AdminProfileRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> findPatient(long id) {
    return required("SELECT * FROM patient WHERE id=? AND deleted=0", id);
  }

  public int updatePatient(long id, AdminPatientUpdateRequest request) {
    return jdbc.update(
        "UPDATE patient SET real_name=?,phone=?,address=?,emergency_contact=?,emergency_phone=?"
            + " WHERE id=? AND deleted=0",
        request.real_name(),
        request.phone(),
        request.address(),
        request.emergency_contact(),
        request.emergency_phone(),
        id);
  }

  public Map<String, Object> findPatientAfterUpdate(long id) {
    return queryOne("SELECT * FROM patient WHERE id=?", id);
  }

  public Map<String, Object> findDoctor(long id) {
    return required(
        "SELECT d.*,dp.name department_name FROM doctor d JOIN department dp ON"
            + " dp.id=d.department_id WHERE d.id=? AND d.deleted=0",
        id);
  }

  public int updateDoctor(long id, AdminDoctorUpdateRequest request) {
    return jdbc.update(
        "UPDATE doctor SET"
            + " department_id=?,real_name=?,title=?,specialty=?,introduction=?,avatar_url=?,consultation_fee=?"
            + " WHERE id=? AND deleted=0",
        request.department_id(),
        request.real_name(),
        request.title(),
        request.specialty(),
        request.introduction(),
        request.avatar_url(),
        request.consultation_fee(),
        id);
  }

  public Map<String, Object> findDoctorAfterUpdate(long id) {
    return queryOne(
        "SELECT d.*,dp.name department_name FROM doctor d JOIN department dp ON"
            + " dp.id=d.department_id WHERE d.id=?",
        id);
  }

  public int updateDoctorStatus(long id, int status) {
    return jdbc.update("UPDATE doctor SET status=? WHERE id=? AND deleted=0", status, id);
  }

  public void insertPatientOperationLog(
      long userId, String type, long targetId, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'patient',?,?,?)",
        userId,
        type,
        targetId,
        description,
        ipAddress);
  }

  public void insertDoctorOperationLog(
      long userId, String type, long targetId, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'doctor',?,?,?)",
        userId,
        type,
        targetId,
        description,
        ipAddress);
  }

  private Map<String, Object> required(String sql, Object... args) {
    Map<String, Object> value = queryOne(sql, args);
    if (value == null) throw new EmptyResultDataAccessException(1);
    return value;
  }

  private Map<String, Object> queryOne(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> result = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      result.put(metadata.getColumnLabel(i), rs.getObject(i));
    return result;
  }
}
