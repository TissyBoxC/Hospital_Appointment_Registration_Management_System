package io.github.tissyboxc.harmsys.doctor.controller;

import io.github.tissyboxc.harmsys.doctor.service.ScheduleRequestService;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/** 管理员和拥有排班申请审核权限的用户审核医生排班申请。 */
@RestController
@RequestMapping("/api/schedule-requests")
public class ScheduleRequestReviewController {
  private final ScheduleRequestService service;

  public ScheduleRequestReviewController(ScheduleRequestService service) {
    this.service = service;
  }

  /** 查询可审核的排班申请。 */
  @GetMapping
  public java.util.List<ScheduleRequestResult> list(
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) Long department_id,
      HttpServletRequest request) {
    return service.reviewList(status, department_id, request);
  }

  /** 通过排班申请。 */
  @PostMapping("/{id}/approve")
  public ScheduleRequestResult approve(
      @PathVariable long id,
      @Valid @RequestBody(required = false) ReviewBody body,
      HttpServletRequest request) {
    return service.review(id, true, body == null ? null : body.review_remark(), request);
  }

  /** 驳回排班申请。 */
  @PostMapping("/{id}/reject")
  public ScheduleRequestResult reject(
      @PathVariable long id,
      @Valid @RequestBody ReviewBody body,
      HttpServletRequest request) {
    return service.review(id, false, body.review_remark(), request);
  }

  /** 审核意见请求参数。 */
  public record ReviewBody(@Size(max = 500) String review_remark) {}
}

