package io.github.tissyboxc.harmsys.pharmacy.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 药房处方查询、发药和库存扣减的数据访问层。 */
@Repository
public class PharmacyRepository {
  private final JdbcTemplate jdbc;

  public PharmacyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> prescriptions(Integer status, Integer paymentStatus) {
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE p.status IN (2,3)");
    if (status != null) {
      where.append(" AND p.status=?");
      args.add(status);
    }
    if (paymentStatus != null) {
      where.append(" AND p.payment_status=?");
      args.add(paymentStatus);
    }
    return jdbc.queryForList(
        "SELECT p.*,v.patient_id,pt.real_name patient_name,pt.phone patient_phone,d.real_name"
            + " doctor_name FROM prescription p JOIN medical_visit v ON v.id=p.visit_id JOIN"
            + " patient pt ON pt.id=v.patient_id JOIN doctor d ON d.id=p.doctor_id"
            + where
            + " ORDER BY p.created_at DESC",
        args.toArray());
  }

  public Map<String, Object> findPrescription(long id) {
    return one(
        "SELECT p.*,v.patient_id,pt.real_name patient_name,pt.phone patient_phone,d.real_name"
            + " doctor_name FROM prescription p JOIN medical_visit v ON v.id=p.visit_id JOIN"
            + " patient pt ON pt.id=v.patient_id JOIN doctor d ON d.id=p.doctor_id WHERE"
            + " p.id=?",
        id);
  }

  public List<Map<String, Object>> prescriptionItems(long prescriptionId) {
    return jdbc.queryForList(
        "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", prescriptionId);
  }

  public Map<String, Object> lockDispensablePrescription(long id) {
    return one(
        "SELECT * FROM prescription WHERE id=? AND status=2 AND payment_status=2 FOR UPDATE", id);
  }

  public List<Map<String, Object>> lockDispenseItems(long prescriptionId) {
    return jdbc.queryForList(
        "SELECT i.*,m.stock_quantity FROM prescription_item i"
            + " LEFT JOIN medicine m ON m.id=i.medicine_id"
            + " WHERE i.prescription_id=? ORDER BY i.id",
        prescriptionId);
  }

  public int deductStock(long medicineId, Object quantity) {
    return jdbc.update(
        "UPDATE medicine SET stock_quantity=stock_quantity-?"
            + " WHERE id=? AND stock_quantity>=?",
        quantity,
        medicineId,
        quantity);
  }

  public int markDispensed(long id) {
    return jdbc.update("UPDATE prescription SET status=3 WHERE id=?", id);
  }

  public void insertOperationLog(
      long userId, String type, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        "prescription",
        id,
        description,
        ipAddress);
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
