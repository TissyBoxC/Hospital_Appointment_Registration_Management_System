package io.github.tissyboxc.harmsys.doctor;

import io.github.tissyboxc.harmsys.doctor.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/schedules")
/** 管理员维护医生排班和时间段的接口。 */
public class AdminScheduleController {
  private final DoctorService service;

  public AdminScheduleController(DoctorService service) {
    this.service = service;
  }

  /**
   * 管理员创建医生排班
   * @param body 请求体,包含排班信息
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ScheduleResult create(
      @Valid @RequestBody ScheduleRequest body, HttpServletRequest request) {
    return service.createAdminSchedule(body, request);
  }

  /**
   * 管理员查询所有排班
   */
  @GetMapping
  public java.util.List<ScheduleResult> list(HttpServletRequest request) {
    return service.allSchedules(request);
  }

  /**
   * 根据排班ID获取排班信息
   * @param scheduleId 排班信息
   */
  @GetMapping("/{scheduleId}")
  public ScheduleResult get(@PathVariable long scheduleId, HttpServletRequest request) {
    return service.adminSchedule(scheduleId, request);
  }

  /**
   * 管理员修改排班信息
   * @param scheduleId 排班ID
   * @param body 下游请求体
   */
  @PutMapping("/{scheduleId}")
  public ScheduleResult update(
      @PathVariable long scheduleId,
      @Valid @RequestBody ScheduleRequest body,
      HttpServletRequest request) {
    return service.updateAdminSchedule(scheduleId, body, request);
  }

  /**
   * 管理员获取排班的时间段
   * @param scheduleId 排班ID
   */
  @GetMapping("/{scheduleId}/slots")
  public java.util.List<SlotResult> slots(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.adminSlots(scheduleId, request);
  }

  /**
   * 管理员创建指定排班的时间段信息
   * @param scheduleId 排班ID
   * @param body 请求体
   */
  @PostMapping("/{scheduleId}/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public SlotResult createSlot(
      @PathVariable long scheduleId,
      @Valid @RequestBody SlotRequest body,
      HttpServletRequest request) {
    return service.createAdminSlot(scheduleId, body, request);
  }

  /**
   * 修改指定时间段的状态
   * @param slotId 时间段ID
   * @param body 请求体
   */
  @PutMapping("/slots/{slotId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSlotStatus(
      @PathVariable long slotId,
      @Valid @RequestBody AdminSlotStatusRequest body,
      HttpServletRequest request) {
    service.adminUpdateSlotStatus(slotId, body.status(), request);
  }

  /**
   * 排班删除接口
   * @param scheduleId 排班ID
   */
  @DeleteMapping("/{scheduleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long scheduleId, HttpServletRequest request) {
    service.deleteAdminSchedule(scheduleId, request);
  }

  /** 管理员调整时间段状态的请求。 */
  public record AdminSlotStatusRequest(
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.Min(0)
          @jakarta.validation.constraints.Max(2)
          Integer status) {}
}
