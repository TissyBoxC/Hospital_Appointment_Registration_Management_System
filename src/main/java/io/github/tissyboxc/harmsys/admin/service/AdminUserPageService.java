package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserPageQuery;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.Map;

/** 管理员用户分页查询业务逻辑。 */
public interface AdminUserPageService {
public Map<String, Object> page(AuthenticatedUser operator, int page, int pageSize, Integer userType, Integer status, String keyword);
}
