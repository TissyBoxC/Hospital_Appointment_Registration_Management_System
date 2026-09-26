package io.github.tissyboxc.harmsys.system.controller;

import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import io.github.tissyboxc.harmsys.system.service.DataConsistencyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 管理员检查和修复业务数据一致性的接口。 */
@RestController
@RequestMapping("/api/admin/data-consistency")
public class DataConsistencyController {
  private final DataConsistencyService service;

  public DataConsistencyController(DataConsistencyService service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> check(HttpServletRequest request) {
    return service.check(SessionAuth.require(request));
  }

  @PostMapping("/repair")
  public Map<String, Object> repair(HttpServletRequest request) {
    return service.repair(SessionAuth.require(request));
  }
}
