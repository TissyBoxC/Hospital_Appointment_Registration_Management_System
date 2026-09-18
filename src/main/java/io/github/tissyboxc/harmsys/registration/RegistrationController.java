package io.github.tissyboxc.harmsys.registration;

import io.github.tissyboxc.harmsys.appointment.AppointmentService;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registration")
/** 挂号员执行预约签到及队列查询的接口。 */
public class RegistrationController {
  private final AppointmentService appointmentService;

  public RegistrationController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  /**
   * 查询预约信息
   */
  @GetMapping("/appointments")
  public List<Map<String, Object>> appointments(HttpServletRequest request) {
    return appointmentService.registrationList(request);
  }

  /**
   * 分页查询预约信息
   * @param status 状态
   */
  @GetMapping("/appointments/page")
  public Map<String, Object> appointmentsPage(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Integer status,
      HttpServletRequest request) {
    return appointmentService.registrationPage(request, page, page_size, status);
  }

  /**
   * 查询预约队列
   */
  @GetMapping("/queue")
  public List<Map<String, Object>> queue(HttpServletRequest request) {
    return appointmentService.queue(request);
  }

  /**
   * 根据预约订单进行签到
   * @param id 预约ID
   */
  @PostMapping("/appointments/{id}/check-in")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void checkIn(@PathVariable long id, HttpServletRequest request) {
    var user = SessionAuth.require(request);
    appointmentService.checkIn(
        id, null, user.user_id(), "REGISTRATION_CHECK_IN", request.getRemoteAddr());
  }

  /**
   * 根据预约ID进行取消订单
   * @param body 取消请求体,包含可选原因
   */
  @PostMapping("/appointments/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(
      @PathVariable long id,
      @Valid @RequestBody(required = false) CancelRequest body,
      HttpServletRequest request) {
    appointmentService.cancelByRegistration(id, body == null ? null : body.reason(), request);
  }

  /** 挂号员取消预约时提交的原因。 */
  public record CancelRequest(@Size(max = 255) String reason) {}
}
