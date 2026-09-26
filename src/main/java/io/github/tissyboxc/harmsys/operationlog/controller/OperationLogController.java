package io.github.tissyboxc.harmsys.operationlog.controller;

import io.github.tissyboxc.harmsys.operationlog.service.OperationLogService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 管理员分页查询系统操作日志的接口。 */
@RestController
@RequestMapping("/api/admin/operation-logs")
public class OperationLogController {
  private final OperationLogService service;

  public OperationLogController(OperationLogService service) {
    this.service = service;
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Long user_id,
      @RequestParam(required = false) String operation_type,
      @RequestParam(required = false) String target_type,
      @RequestParam(required = false) Long target_id,
      @RequestParam(required = false) String start_time,
      @RequestParam(required = false) String end_time,
      @RequestParam(defaultValue = "100") int limit,
      HttpServletRequest request) {
    return service.list(
        SessionAuth.require(request),
        user_id,
        operation_type,
        target_type,
        target_id,
        start_time,
        end_time,
        limit);
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    return service.get(SessionAuth.require(request), id);
  }

  @GetMapping("/paged")
  public Map<String, Object> paged(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      HttpServletRequest request) {
    return service.paged(SessionAuth.require(request), page, page_size);
  }
}

