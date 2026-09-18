package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.AdminUserSummary;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
/** 管理员查询用户、角色和权限摘要的接口。 */
public class AdminQueryController {
  private final AdminQueryService service;

  public AdminQueryController(AdminQueryService service) {
    this.service = service;
  }

  /**
   * 管理员查询用户
   */
  @GetMapping
  public List<AdminUserSummary> users(HttpServletRequest r) {
    return service.users(r);
  }

  /**
   * 按ID查询用户
   * @param id 用户ID
   */
  @GetMapping("/{id}")
  public AdminUserSummary user(@PathVariable long id, HttpServletRequest r) {
    return service.user(id, r);
  }

  /**
   * 查询用户的角色信息
   */
  @GetMapping("/{id}/roles")
  public List<String> roles(@PathVariable long id, HttpServletRequest r) {
    return service.roles(id, r);
  }

  /**
   * 查询用户权限
   */
  @GetMapping("/{id}/permissions")
  public List<String> permissions(@PathVariable long id, HttpServletRequest r) {
    return service.permissions(id, r);
  }
}
