package io.github.tissyboxc.harmsys.department;

import io.github.tissyboxc.harmsys.department.dto.DepartmentRequest;
import io.github.tissyboxc.harmsys.department.dto.DepartmentResult;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
/** 科室及其层级关系的数据访问层。 */
public class DepartmentRepository {
  private final JdbcTemplate jdbcTemplate;

  public DepartmentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 查询所有科室,可选科室状态
   * @param onlyEnabled 科室是否启用
   * @return 科室信息
   */
  public List<DepartmentResult> findAll(boolean onlyEnabled) {
    String condition = onlyEnabled ? "AND status = 1" : "";
    return jdbcTemplate.query(
        "SELECT"
            + " id,parent_id,name,code,description,location,contact_phone,sort_no,status,created_at,updated_at"
            + " FROM department WHERE deleted = 0 "
            + condition
            + " ORDER BY sort_no,id",
        this::map);
  }

  /**
   * 根据ID查询科室
   * @param id 科室ID
   * @return 科室信息
   */
  public Optional<DepartmentResult> findById(long id) {
    return jdbcTemplate
        .query(
            "SELECT"
                + " id,parent_id,name,code,description,location,contact_phone,sort_no,status,created_at,updated_at"
                + " FROM department WHERE id = ? AND deleted = 0",
            this::map,
            id)
        .stream()
        .findFirst();
  }

  /**
   * 某科室是否存在
   * @param id 科室ID
   */
  public boolean exists(long id) {
    return count("SELECT COUNT(*) FROM department WHERE id = ? AND deleted = 0", id) > 0;
  }

  /**
   * 某科室是否有子科室
   */
  public boolean hasChildren(long id) {
    return count("SELECT COUNT(*) FROM department WHERE parent_id = ? AND deleted = 0", id) > 0;
  }

  /**
   * 某科室是否有医生
   */
  public boolean hasDoctors(long id) {
    return count("SELECT COUNT(*) FROM doctor WHERE department_id = ? AND deleted = 0", id) > 0;
  }

  /**
   * 插入科室信息
   * @param request 科室信息
   */
  public long insert(DepartmentRequest request) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    int rows =
        jdbcTemplate.update(
            connection -> {
              PreparedStatement ps =
                  connection.prepareStatement(
                      "INSERT INTO"
                          + " department(parent_id,name,code,description,location,contact_phone,sort_no,status,deleted)"
                          + " VALUES(?,?,?,?,?,?,?, ?,0)",
                      Statement.RETURN_GENERATED_KEYS);
              ps.setObject(1, request.parent_id());
              ps.setString(2, request.name());
              ps.setString(3, request.code());
              ps.setString(4, request.description());
              ps.setString(5, request.location());
              ps.setString(6, request.contact_phone());
              ps.setInt(7, request.sort_no());
              ps.setInt(8, request.status());
              return ps;
            },
            keyHolder);
    if (rows != 1 || keyHolder.getKey() == null) throw new IllegalStateException("创建科室失败");
    return keyHolder.getKey().longValue();
  }

  public void update(long id, DepartmentRequest request) {
    if (jdbcTemplate.update(
            "UPDATE department SET"
                + " parent_id=?,name=?,code=?,description=?,location=?,contact_phone=?,sort_no=?,status=?"
                + " WHERE id=? AND deleted=0",
            request.parent_id(),
            request.name(),
            request.code(),
            request.description(),
            request.location(),
            request.contact_phone(),
            request.sort_no(),
            request.status(),
            id)
        != 1) throw new IllegalStateException("科室不存在或已删除");
  }

  public void logicalDelete(long id) {
    if (jdbcTemplate.update("UPDATE department SET deleted=1,status=0 WHERE id=? AND deleted=0", id)
        != 1) throw new IllegalStateException("科室不存在或已删除");
  }

  /**
   * 写日志
   * @param userId 操作者ID
   * @param operation 操作
   * @param targetId 目标ID
   * @param description 描述
   * @param ip 地址
   */
  public void writeLog(
      long userId, String operation, Long targetId, String description, String ip) {
    jdbcTemplate.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'department',?,?,?)",
        userId,
        operation,
        targetId,
        description,
        ip);
  }

  private long count(String sql, Object... args) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  /**
   * 格式化处理数据库返回的科室信息
   * @param rs 原始信息
   * @param row 行数
   * @return 格式化后的科室实体
   */
  private DepartmentResult map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new DepartmentResult(
        rs.getLong("id"),
        getNullableLong(rs, "parent_id"),
        rs.getString("name"),
        rs.getString("code"),
        rs.getString("description"),
        rs.getString("location"),
        rs.getString("contact_phone"),
        rs.getInt("sort_no"),
        rs.getInt("status"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime());
  }

  /**
   * 可NULL数据处理为JAVA中的null
   */
  private Long getNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }
}
