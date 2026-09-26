package io.github.tissyboxc.harmsys.announcement.controller;

import io.github.tissyboxc.harmsys.announcement.service.AnnouncementService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 公告查询和维护接口。 */
@RestController
public class AnnouncementController {
  private final AnnouncementService service;

  public AnnouncementController(AnnouncementService service) {
    this.service = service;
  }

  @GetMapping("/api/public/announcements")
  public List<Map<String, Object>> publicList() {
    return service.publicList();
  }

  @GetMapping("/api/public/announcements/{id}")
  public Map<String, Object> publicGet(@PathVariable long id) {
    return service.publicGet(id);
  }

  @GetMapping("/api/admin/announcements")
  public List<Map<String, Object>> adminList(HttpServletRequest request) {
    return service.adminList(SessionAuth.require(request));
  }

  @PostMapping("/api/admin/announcements")
  public Map<String, Object> create(
      @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.create(SessionAuth.require(request), body);
  }

  @PutMapping("/api/admin/announcements/{id}")
  public Map<String, Object> update(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.update(SessionAuth.require(request), id, body);
  }

  @PostMapping("/api/admin/announcements/{id}/publish")
  public Map<String, Object> publish(@PathVariable long id, HttpServletRequest request) {
    return service.publish(SessionAuth.require(request), id);
  }

  @PostMapping("/api/admin/announcements/{id}/retract")
  public Map<String, Object> retract(@PathVariable long id, HttpServletRequest request) {
    return service.retract(SessionAuth.require(request), id);
  }
}

