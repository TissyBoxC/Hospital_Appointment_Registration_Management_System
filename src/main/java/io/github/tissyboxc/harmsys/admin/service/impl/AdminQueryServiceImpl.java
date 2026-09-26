package io.github.tissyboxc.harmsys.admin.service.impl;

import io.github.tissyboxc.harmsys.admin.service.AdminQueryService;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserSummary;
import io.github.tissyboxc.harmsys.admin.mapper.AdminQueryMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
/** 管理员查询用户角色和权限信息的业务服务。 */
public class AdminQueryServiceImpl implements AdminQueryService {
  private final AdminQueryMapper mapper;

  public AdminQueryServiceImpl(AdminQueryMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * 验证已登录且管理员
   */
  private void admin(HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以查询");
  }

  /**
   * 查询所有用户
   */
  @Override
  public List<AdminUserSummary> users(HttpServletRequest r) {
    admin(r);
    return mapper.selectUsers().stream().map(this::summary).toList();
  }

  /**
   * 按ID查询用户
   * @param id 用户ID
   */
  @Override
  public AdminUserSummary user(long id, HttpServletRequest r) {
    admin(r);
    Map<String, Object> row = mapper.selectUser(id);
    if (row == null) throw new UserRegistrationException(404, "用户不存在");
    return summary(row);
  }

  /**
   * 按ID查询用户角色
   */
  @Override
  public List<String> roles(long id, HttpServletRequest r) {
    admin(r);
    return mapper.selectRoles(id);
  }

  /**
   * 按ID查询用户权限
   */
  @Override
  public List<String> permissions(long id, HttpServletRequest r) {
    admin(r);
    return mapper.selectPermissions(id);
  }

  private AdminUserSummary summary(Map<String, Object> row) {
    long userId = number(row, "user_id");
    return new AdminUserSummary(
        userId,
        text(row, "username"),
        integer(row, "user_type"),
        integer(row, "status"),
        text(row, "display_name"),
        nullableNumber(row, "patient_id"),
        nullableNumber(row, "doctor_id"),
        nullableNumber(row, "department_id"),
        text(row, "department_name"),
        mapper.selectRoles(userId),
        mapper.selectPermissions(userId));
  }

  private long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  private Long nullableNumber(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? null : ((Number) value).longValue();
  }

  private Integer integer(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? null : ((Number) value).intValue();
  }

  private String text(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? null : String.valueOf(value);
  }
}

