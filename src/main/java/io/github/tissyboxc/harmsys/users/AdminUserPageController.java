package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
/** 管理员用户分页与筛选查询。 */
public class AdminUserPageController {
  private final JdbcTemplate jdbc;

  public AdminUserPageController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer user_type,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    admin(request);
    int pg = Math.max(1, page),
        size = Math.min(Math.max(1, page_size), 100),
        offset = (pg - 1) * size;
    StringBuilder where = new StringBuilder(" WHERE u.deleted=0");
    List<Object> args = new ArrayList<>();
    if (user_type != null) {
      where.append(" AND u.user_type=?");
      args.add(user_type);
    }
    if (status != null) {
      where.append(" AND u.status=?");
      args.add(status);
    }
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (u.username LIKE ? OR p.real_name LIKE ? OR d.real_name LIKE ?)");
      String k = "%" + keyword.trim() + "%";
      args.add(k);
      args.add(k);
      args.add(k);
    }
    String base =
        " FROM sys_user u LEFT JOIN patient p ON p.user_id=u.id AND p.deleted=0 LEFT JOIN doctor d"
            + " ON d.user_id=u.id AND d.deleted=0";
    List<Object> queryArgs = new ArrayList<>(args);
    queryArgs.add(size);
    queryArgs.add(offset);
    List<Map<String, Object>> items =
        jdbc.queryForList(
            "SELECT u.id"
                + " user_id,u.username,u.user_type,u.status,COALESCE(p.real_name,d.real_name,u.username)"
                + " display_name,p.id patient_id,d.id doctor_id"
                + base
                + where
                + " ORDER BY u.id DESC LIMIT ? OFFSET ?",
            queryArgs.toArray());
    long total = jdbc.queryForObject("SELECT COUNT(*)" + base + where, Long.class, args.toArray());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("page", pg);
    out.put("page_size", size);
    out.put("total", total);
    out.put("items", items);
    return out;
  }

  private void admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询用户");
  }
}
