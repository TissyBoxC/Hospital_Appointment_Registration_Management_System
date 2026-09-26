package io.github.tissyboxc.harmsys.clinical.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 医疗附件持久化访问。 */
@Repository
public class MedicalAttachmentRepository {
  private final JdbcTemplate jdbc;

  public MedicalAttachmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(
      long patientId,
      Long visitId,
      long uploaderUserId,
      String attachmentType,
      String originalName,
      String storedName,
      String filePath,
      String contentType,
      long fileSize,
      String description) {
    jdbc.update(
        "INSERT INTO"
            + " medical_attachment(patient_id,visit_id,uploader_user_id,attachment_type,original_name,stored_name,file_path,content_type,file_size,description)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
        patientId,
        visitId,
        uploaderUserId,
        attachmentType,
        originalName,
        storedName,
        filePath,
        contentType,
        fileSize,
        description);
    Long id =
        jdbc.queryForObject(
            "SELECT id FROM medical_attachment WHERE stored_name=?", Long.class, storedName);
    if (id == null) throw new IllegalStateException("保存附件记录失败");
    return id;
  }

  public Map<String, Object> findById(long id) {
    return one("SELECT * FROM medical_attachment WHERE id=?", id);
  }

  public List<Map<String, Object>> listByPatient(long patientId, Long visitId) {
    String sql =
        "SELECT"
            + " id,patient_id,visit_id,uploader_user_id,attachment_type,original_name,content_type,file_size,description,created_at"
            + " FROM medical_attachment WHERE patient_id=?";
    if (visitId == null)
      return jdbc.queryForList(sql + " ORDER BY created_at DESC", patientId);
    return jdbc.queryForList(
        sql + " AND visit_id=? ORDER BY created_at DESC", patientId, visitId);
  }

  public boolean patientExists(long patientId) {
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM patient WHERE id=? AND deleted=0", Long.class, patientId);
    return count != null && count > 0;
  }

  public int delete(long id) {
    return jdbc.update("DELETE FROM medical_attachment WHERE id=?", id);
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
