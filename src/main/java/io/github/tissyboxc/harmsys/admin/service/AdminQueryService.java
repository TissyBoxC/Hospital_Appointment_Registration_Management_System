package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserSummary;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/** 管理员查询用户角色和权限信息的业务服务。 */
public interface AdminQueryService {
public List<AdminUserSummary> users(HttpServletRequest r);

  public AdminUserSummary user(long id, HttpServletRequest r);

  public List<String> roles(long id, HttpServletRequest r);

  public List<String> permissions(long id, HttpServletRequest r);
}
