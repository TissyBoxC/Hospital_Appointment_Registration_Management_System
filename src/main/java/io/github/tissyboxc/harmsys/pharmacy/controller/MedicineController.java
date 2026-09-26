package io.github.tissyboxc.harmsys.pharmacy.controller;

import io.github.tissyboxc.harmsys.pharmacy.dto.MedicineRequest;
import io.github.tissyboxc.harmsys.pharmacy.dto.StockInRequest;
import io.github.tissyboxc.harmsys.pharmacy.service.MedicineService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 药品库存维护和医生端药品检索接口。 */
@RestController
@RequestMapping("/api")
public class MedicineController {
  private final MedicineService service;

  public MedicineController(MedicineService service) {
    this.service = service;
  }

  @GetMapping("/doctor/medicines")
  public List<Map<String, Object>> doctorSearch(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    return service.doctorSearch(SessionAuth.require(request), keyword);
  }

  @GetMapping("/pharmacy/medicines")
  public Map<String, Object> pharmacyList(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "false") boolean low_stock_only,
      HttpServletRequest request) {
    return service.pharmacyList(
        SessionAuth.require(request), page, page_size, keyword, status, low_stock_only);
  }

  @GetMapping("/pharmacy/medicines/{id}")
  public Map<String, Object> detail(@PathVariable long id, HttpServletRequest request) {
    return service.detail(SessionAuth.require(request), id);
  }

  @PostMapping("/pharmacy/medicines")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> create(
      @Valid @RequestBody MedicineRequest body, HttpServletRequest request) {
    return service.create(SessionAuth.require(request), body, request.getRemoteAddr());
  }

  @PutMapping("/pharmacy/medicines/{id}")
  public Map<String, Object> update(
      @PathVariable long id,
      @Valid @RequestBody MedicineRequest body,
      HttpServletRequest request) {
    return service.update(SessionAuth.require(request), id, body, request.getRemoteAddr());
  }

  @PostMapping("/pharmacy/medicines/{id}/stock-in")
  public Map<String, Object> stockIn(
      @PathVariable long id,
      @Valid @RequestBody StockInRequest body,
      HttpServletRequest request) {
    return service.stockIn(SessionAuth.require(request), id, body, request.getRemoteAddr());
  }
}

