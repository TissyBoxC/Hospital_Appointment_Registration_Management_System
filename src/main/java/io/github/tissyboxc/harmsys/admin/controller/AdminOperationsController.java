package io.github.tissyboxc.harmsys.admin.controller;

import io.github.tissyboxc.harmsys.admin.service.AdminOperationsService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 管理员维护用户、角色、权限并查询统计的接口。 */
@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {
  private final AdminOperationsService service;

  public AdminOperationsController(AdminOperationsService service) {
    this.service = service;
  }

  @PutMapping("/users/{id}")
  public Map<String, Object> updateUser(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.updateUser(SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @DeleteMapping("/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable long id, HttpServletRequest request) {
    service.deleteUser(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/users/{id}/restore")
  public Map<String, Object> restoreUser(@PathVariable long id, HttpServletRequest request) {
    return service.restoreUser(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/users/{userId}/roles")
  public Map<String, Object> assignRole(
      @PathVariable long userId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    return service.assignRole(
        SessionAuth.require(request), userId, body, request.getRemoteAddr());
  }

  @DeleteMapping("/users/{userId}/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeRole(
      @PathVariable long userId, @PathVariable long roleId, HttpServletRequest request) {
    service.removeRole(SessionAuth.require(request), userId, roleId, request.getRemoteAddr());
  }

  @PostMapping("/roles/{roleId}/permissions")
  public Map<String, Object> assignPermission(
      @PathVariable long roleId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    return service.assignRolePermission(
        SessionAuth.require(request), roleId, body, request.getRemoteAddr());
  }

  @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removePermission(
      @PathVariable long roleId,
      @PathVariable long permissionId,
      HttpServletRequest request) {
    service.removeRolePermission(
        SessionAuth.require(request), roleId, permissionId, request.getRemoteAddr());
  }

  @PostMapping("/users/{userId}/permissions")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> assignUserPermission(
      @PathVariable long userId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    return service.assignUserPermission(
        SessionAuth.require(request), userId, body, request.getRemoteAddr());
  }

  @DeleteMapping("/users/{userId}/permissions/{permissionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeUserPermission(
      @PathVariable long userId,
      @PathVariable long permissionId,
      HttpServletRequest request) {
    service.removeUserPermission(
        SessionAuth.require(request), userId, permissionId, request.getRemoteAddr());
  }

  @GetMapping("/users/{userId}/direct-permissions")
  public List<Map<String, Object>> userDirectPermissions(
      @PathVariable long userId, HttpServletRequest request) {
    return service.userDirectPermissions(SessionAuth.require(request), userId);
  }

  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createRole(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.createRole(SessionAuth.require(request), body, request.getRemoteAddr());
  }

  @GetMapping("/roles")
  public List<Map<String, Object>> roles(HttpServletRequest request) {
    return service.roles(SessionAuth.require(request));
  }

  @GetMapping("/roles/{id}/permissions")
  public List<Map<String, Object>> rolePermissions(
      @PathVariable long id, HttpServletRequest request) {
    return service.rolePermissions(SessionAuth.require(request), id);
  }

  @PutMapping("/roles/{id}")
  public Map<String, Object> updateRole(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.updateRole(SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @DeleteMapping("/roles/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRole(@PathVariable long id, HttpServletRequest request) {
    service.disableRole(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @PostMapping("/permissions")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createPermission(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.createPermission(SessionAuth.require(request), body, request.getRemoteAddr());
  }

  @GetMapping("/permissions")
  public List<Map<String, Object>> permissions(HttpServletRequest request) {
    return service.permissions(SessionAuth.require(request));
  }

  @PutMapping("/permissions/{id}")
  public Map<String, Object> updatePermission(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.updatePermission(
        SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @DeleteMapping("/permissions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePermission(@PathVariable long id, HttpServletRequest request) {
    service.deletePermission(SessionAuth.require(request), id, request.getRemoteAddr());
  }

  @GetMapping("/statistics")
  public Map<String, Object> statistics(HttpServletRequest request) {
    return service.statistics(SessionAuth.require(request));
  }
}

