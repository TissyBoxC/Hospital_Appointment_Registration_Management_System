package io.github.tissyboxc.harmsys.admin.repository;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserPageQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 管理员用户分页查询的数据访问层。 */
@Repository
public class AdminUserPageRepository {
  private final JdbcTemplate jdbc;

  public AdminUserPageRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> items(AdminUserPageQuery query, int offset) {
    List<Object> args = new ArrayList<>(filterArgs(query));
    args.add(query.pageSize());
    args.add(offset);
    return jdbc.queryForList(
        "SELECT u.id"
            + " user_id,u.username,u.user_type,u.status,COALESCE(p.real_name,d.real_name,u.username)"
            + " display_name,p.id patient_id,d.id doctor_id,d.department_id,dp.name department_name"
            + baseSql()
            + whereSql(query)
            + " ORDER BY u.id DESC LIMIT ? OFFSET ?",
        args.toArray());
  }

  public long total(AdminUserPageQuery query) {
    Long value =
        jdbc.queryForObject(
            "SELECT COUNT(*)" + baseSql() + whereSql(query),
            Long.class,
            filterArgs(query).toArray());
    return value == null ? 0 : value;
  }

  private String baseSql() {
    return " FROM sys_user u LEFT JOIN patient p ON p.user_id=u.id AND p.deleted=0 LEFT JOIN doctor"
        + " d ON d.user_id=u.id AND d.deleted=0 LEFT JOIN department dp ON dp.id=d.department_id";
  }

  private String whereSql(AdminUserPageQuery query) {
    StringBuilder where = new StringBuilder(" WHERE u.deleted=0");
    if (query.userType() != null) where.append(" AND u.user_type=?");
    if (query.status() != null) where.append(" AND u.status=?");
    if (query.keyword() != null && !query.keyword().isBlank())
      where.append(" AND (u.username LIKE ? OR p.real_name LIKE ? OR d.real_name LIKE ?)");
    return where.toString();
  }

  private List<Object> filterArgs(AdminUserPageQuery query) {
    List<Object> args = new ArrayList<>();
    if (query.userType() != null) args.add(query.userType());
    if (query.status() != null) args.add(query.status());
    if (query.keyword() != null && !query.keyword().isBlank()) {
      String keyword = "%" + query.keyword().trim() + "%";
      args.add(keyword);
      args.add(keyword);
      args.add(keyword);
    }
    return args;
  }
}
