package io.github.tissyboxc.harmsys.pharmacy.controller;

import io.github.tissyboxc.harmsys.pharmacy.service.PharmacyService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 药房端处方查询、发药确认和库存扣减。 */
@RestController
@RequestMapping("/api/pharmacy")
/** 药房查看处方并更新发药状态的接口。 */
public class PharmacyController {
  private final PharmacyService service;

  public PharmacyController(PharmacyService service) {
    this.service = service;
  }

  /**
   * 查询所有处方
   * @param status 状态
   */
  @GetMapping("/prescriptions")
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Integer payment_status,
      HttpServletRequest request) {
    return service.prescriptions(SessionAuth.require(request), status, payment_status);
  }

  /**
   * 根据ID查询处方
   * @param id 处方ID
   */
  @GetMapping("/prescriptions/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    return service.prescription(SessionAuth.require(request), id);
  }

  /**
   *处方标记已取药
   */
  @PostMapping("/prescriptions/{id}/dispense")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dispense(@PathVariable long id, HttpServletRequest request) {
    service.dispense(SessionAuth.require(request), id, request.getRemoteAddr());
  }
}


