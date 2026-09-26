package io.github.tissyboxc.harmsys.pharmacy.repository;

import io.github.tissyboxc.harmsys.pharmacy.dto.MedicineRequest;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 药品和库存持久化访问。 */
@Repository
public class MedicineRepository {
  private final JdbcTemplate jdbc;

  public MedicineRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> candidates(Integer status, boolean lowStockOnly) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null) {
      where.append(" AND status=?");
      args.add(status);
    }
    if (lowStockOnly) where.append(" AND stock_quantity <= warning_quantity");
    return jdbc.queryForList(
        "SELECT m.*,(m.stock_quantity<=m.warning_quantity) AS low_stock,"
            + "CONCAT(m.stock_quantity,' ',m.unit) AS stock_text FROM medicine m"
            + where
            + " ORDER BY m.name,m.id",
        args.toArray());
  }

  public List<Map<String, Object>> searchable() {
    return jdbc.queryForList(
        "SELECT m.id,m.medicine_code,m.name,m.specification,m.unit,m.unit_price,"
            + "m.stock_quantity,m.warning_quantity,m.manufacturer,"
            + "(m.stock_quantity<=m.warning_quantity) AS low_stock,"
            + "CONCAT(m.stock_quantity,' ',m.unit) AS stock_text"
            + " FROM medicine m WHERE m.status=1 AND m.stock_quantity>0 ORDER BY m.name,m.id");
  }

  public Map<String, Object> findById(long id) {
    return one(
        "SELECT m.*,(m.stock_quantity<=m.warning_quantity) AS low_stock,"
            + "CONCAT(m.stock_quantity,' ',m.unit) AS stock_text FROM medicine m WHERE m.id=?",
        id);
  }

  public long insert(MedicineRequest body) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO medicine(medicine_code,name,specification,unit,unit_price,"
                      + "stock_quantity,warning_quantity,manufacturer,status,remark)"
                      + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, body.medicine_code().trim());
          statement.setString(2, body.name().trim());
          statement.setString(3, body.specification());
          statement.setString(4, body.unit());
          statement.setBigDecimal(5, body.unit_price());
          statement.setBigDecimal(6, body.stock_quantity());
          statement.setBigDecimal(7, body.warning_quantity());
          statement.setString(8, body.manufacturer());
          statement.setInt(9, body.status());
          statement.setString(10, body.remark());
          return statement;
        },
        keyHolder);
    if (keyHolder.getKey() == null) throw new IllegalStateException("新增药品失败");
    return keyHolder.getKey().longValue();
  }

  public int update(long id, MedicineRequest body) {
    return jdbc.update(
        "UPDATE medicine SET medicine_code=?,name=?,specification=?,unit=?,unit_price=?,"
            + "stock_quantity=?,warning_quantity=?,manufacturer=?,status=?,remark=? WHERE id=?",
        body.medicine_code().trim(),
        body.name().trim(),
        body.specification(),
        body.unit(),
        body.unit_price(),
        body.stock_quantity(),
        body.warning_quantity(),
        body.manufacturer(),
        body.status(),
        body.remark(),
        id);
  }

  public int stockIn(long id, BigDecimal quantity) {
    return jdbc.update(
        "UPDATE medicine SET stock_quantity=stock_quantity+? WHERE id=?", quantity, id);
  }

  public void insertOperationLog(
      long userId, String type, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,target_id,description,"
            + "ip_address) VALUES(?,?,'medicine',?,?,?)",
        userId,
        type,
        id,
        description,
        ipAddress);
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
