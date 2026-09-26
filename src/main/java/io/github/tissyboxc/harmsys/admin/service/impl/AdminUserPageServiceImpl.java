package io.github.tissyboxc.harmsys.admin.service.impl;

import io.github.tissyboxc.harmsys.admin.service.AdminUserPageService;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserPageQuery;
import io.github.tissyboxc.harmsys.admin.mapper.AdminQueryMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员用户分页查询业务逻辑。 */
@Service
public class AdminUserPageServiceImpl implements AdminUserPageService {
  private final AdminQueryMapper mapper;

  public AdminUserPageServiceImpl(AdminQueryMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> page(
      AuthenticatedUser operator,
      int page,
      int pageSize,
      Integer userType,
      Integer status,
      String keyword) {
    requireAdmin(operator);
    int normalizedPage = Math.max(1, page);
    int normalizedSize = Math.min(Math.max(1, pageSize), 100);
    int offset = (normalizedPage - 1) * normalizedSize;
    AdminUserPageQuery query =
        new AdminUserPageQuery(normalizedPage, normalizedSize, userType, status, keyword);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", normalizedPage);
    result.put("page_size", normalizedSize);
    result.put("total", mapper.countUserPage(query));
    result.put("items", mapper.selectUserPage(query, offset));
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以查询用户");
  }
}

