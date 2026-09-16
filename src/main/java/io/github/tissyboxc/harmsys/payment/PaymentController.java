package io.github.tissyboxc.harmsys.payment;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
/** 患者发起支付和管理员退款接口。 */
public class PaymentController {
  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  @PostMapping("/patient/appointments/{appointmentId}/pay")
  public java.util.Map<String, Object> pay(@PathVariable long appointmentId, HttpServletRequest r) {
    return service.pay(appointmentId, r);
  }

  @GetMapping("/patient/payments/{id}")
  public java.util.Map<String, Object> get(@PathVariable long id, HttpServletRequest r) {
    return service.get(id, r);
  }

  @PostMapping("/patient/payments/{id}/refund")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refund(
      @PathVariable long id,
      @RequestBody(required = false) java.util.Map<String, Object> body,
      HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    service.refund(
        id, r, body == null ? "患者申请退款" : String.valueOf(body.getOrDefault("reason", "患者申请退款")));
  }

  @PostMapping("/admin/payments/{id}/refund")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void adminRefund(
      @PathVariable long id,
      @RequestBody(required = false) java.util.Map<String, Object> body,
      HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以退款");
    service.refund(
        id, r, body == null ? "管理员退款" : String.valueOf(body.getOrDefault("reason", "管理员退款")));
  }
}
