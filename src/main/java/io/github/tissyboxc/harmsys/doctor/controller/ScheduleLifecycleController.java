package io.github.tissyboxc.harmsys.doctor.controller;

import io.github.tissyboxc.harmsys.doctor.service.ScheduleLifecycleService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
/** 排班停诊、批量取消预约和状态生命周期管理。 */
public class ScheduleLifecycleController {
  private final ScheduleLifecycleService service;

  public ScheduleLifecycleController(ScheduleLifecycleService service) {
    this.service = service;
  }

  /**
   * 管理员取消指定排班->停诊
   * @param id 排班ID
   * @param body 包含原因的请求体
   */
  @PostMapping("/api/admin/schedules/{id}/stop")
  public Map<String, Object> adminStop(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    return service.adminStop(
        SessionAuth.require(request),
        id,
        body == null ? "管理员停诊" : String.valueOf(body.getOrDefault("reason", "管理员停诊")),
        request.getRemoteAddr());
  }

  /**
   * 医生停诊
   * @param id 医生ID
   * @param body 包含原因的请求体
   */
  @PostMapping("/api/doctor/schedules/{id}/stop")
  public Map<String, Object> doctorStop(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    return service.doctorStop(
        SessionAuth.require(request),
        id,
        body == null ? "医生停诊" : String.valueOf(body.getOrDefault("reason", "医生停诊")),
        request.getRemoteAddr());
  }

  /**
   * 管理员修改排班状态
   * @param id 排班ID
   * @param body 包含状态码的请求体
   */
  @PutMapping("/api/admin/schedules/{id}/status")
  public Map<String, Object> status(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    return service.updateStatus(
        SessionAuth.require(request),
        id,
        Integer.parseInt(String.valueOf(body.get("status"))),
        request.getRemoteAddr());
  }

  /**
   * 管理员修改时间段信息
   * @param slotId 时间段ID
   * @param body 修改内容
   */
  @PutMapping("/api/admin/schedules/slots/{slotId}")
  public Map<String, Object> updateAdminSlot(
      @PathVariable long slotId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    return service.updateSlot(
        SessionAuth.require(request), slotId, body, request.getRemoteAddr());
  }

  /**
   * 管理员删除指定时间段
   * @param slotId 时间段ID
   */
  @DeleteMapping("/api/admin/schedules/slots/{slotId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAdminSlot(@PathVariable long slotId, HttpServletRequest request) {
    service.deleteSlot(SessionAuth.require(request), slotId, request.getRemoteAddr());
  }
}


