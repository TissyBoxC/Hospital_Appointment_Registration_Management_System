package io.github.tissyboxc.harmsys.payment.controller;

import io.github.tissyboxc.harmsys.payment.service.PaymentQueryService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 支付记录列表、预约支付关联查询和管理员对账接口。 */
@RestController
@RequestMapping("/api")
public class PaymentQueryController {
  private final PaymentQueryService service;

  public PaymentQueryController(PaymentQueryService service) {
    this.service = service;
  }

  @GetMapping("/patient/payments")
  public Map<String, Object> patientPayments(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      HttpServletRequest request) {
    return service.patientPayments(SessionAuth.require(request), page, page_size);
  }

  @GetMapping("/patient/appointments/{appointmentId}/payment")
  public Map<String, Object> appointmentPayment(
      @PathVariable long appointmentId, HttpServletRequest request) {
    return service.appointmentPayment(SessionAuth.require(request), appointmentId);
  }

  @GetMapping("/admin/payments")
  public Map<String, Object> adminPayments(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    return service.adminPayments(SessionAuth.require(request), page, page_size, status);
  }
}

