package io.github.tissyboxc.harmsys.admin.controller;

import io.github.tissyboxc.harmsys.admin.service.AdminUserPageService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
/** 管理员用户分页与筛选查询。 */
public class AdminUserPageController {
  private final AdminUserPageService service;

  public AdminUserPageController(AdminUserPageService service) {
    this.service = service;
  }

  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer user_type,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    return service.page(
        SessionAuth.require(request), page, page_size, user_type, status, keyword);
  }
}

