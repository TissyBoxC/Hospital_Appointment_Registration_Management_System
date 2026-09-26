package io.github.tissyboxc.harmsys.payment.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 支付记录查询。 */
@Repository
public class PaymentQueryRepository {
  private final JdbcTemplate jdbc;

  public PaymentQueryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> patientPayments(long patientId, int size, int offset) {
    return jdbc.queryForList(
        "SELECT * FROM payment_record WHERE patient_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?",
        patientId,
        size,
        offset);
  }

  public long countPatientPayments(long patientId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM payment_record WHERE patient_id=?", Long.class, patientId);
  }

  public Map<String, Object> appointmentPayment(long appointmentId, long patientId) {
    return one(
        "SELECT pr.* FROM payment_record pr JOIN appointment a ON a.id=pr.appointment_id WHERE"
            + " pr.appointment_id=? AND a.patient_id=? ORDER BY pr.id DESC LIMIT 1",
        appointmentId,
        patientId);
  }

  public List<Map<String, Object>> adminPayments(Integer status, int size, int offset) {
    String where = status == null ? "" : " WHERE pr.status=?";
    String sql =
        "SELECT pr.*,p.real_name patient_name,a.appointment_no FROM payment_record pr JOIN"
            + " patient p ON p.id=pr.patient_id JOIN appointment a ON a.id=pr.appointment_id"
            + where
            + " ORDER BY pr.created_at DESC LIMIT ? OFFSET ?";
    if (status == null) return jdbc.queryForList(sql, size, offset);
    return jdbc.queryForList(sql, status, size, offset);
  }

  public long countAdminPayments(Integer status) {
    String sql =
        "SELECT COUNT(*) FROM payment_record pr"
            + (status == null ? "" : " WHERE pr.status=?");
    if (status == null) return jdbc.queryForObject(sql, Long.class);
    return jdbc.queryForObject(sql, Long.class, status);
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
