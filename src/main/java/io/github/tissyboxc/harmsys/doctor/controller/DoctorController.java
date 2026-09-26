package io.github.tissyboxc.harmsys.doctor.controller;

import io.github.tissyboxc.harmsys.doctor.dto.*;
import io.github.tissyboxc.harmsys.doctor.service.DoctorService;
import io.github.tissyboxc.harmsys.doctor.service.ScheduleRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
/** 医生个人资料、排班和时间段维护接口。 */
public class DoctorController {
  private final DoctorService service;
  private final ScheduleRequestService requestService;

  public DoctorController(DoctorService service, ScheduleRequestService requestService) {
    this.service = service;
    this.requestService = requestService;
  }

  /**
   * 医生获取个人资料
   */
  @GetMapping("/profile")
  public DoctorProfileResult profile(HttpServletRequest request) {
    return service.profile(request);
  }

  /**
   * 医生修改个人资料
   * @param body 请求体,包含修改信息
   */
  @PutMapping("/profile")
  public DoctorProfileResult update(
      @Valid @RequestBody DoctorProfileUpdateRequest body, HttpServletRequest request) {
    return service.updateProfile(body, request);
  }

  /**
   * 医生查询自己拍班
   */
  @GetMapping("/schedules")
  public java.util.List<ScheduleResult> schedules(HttpServletRequest request) {
    return service.schedules(request);
  }

  /**
   * 医生查询指定排班的具体信息
   * @param scheduleId 排班ID
   */
  @GetMapping("/schedules/{scheduleId}")
  public ScheduleResult schedule(@PathVariable long scheduleId, HttpServletRequest request) {
    return service.scheduleForDoctor(scheduleId, request);
  }

  /**
   * 医生创建排班信息
   * @param body 下游请求体,包含排班信息
   */
  @PostMapping("/schedules")
  @ResponseStatus(HttpStatus.CREATED)
  public ScheduleRequestResult createSchedule(
      @Valid @RequestBody ScheduleRequestSubmit body, HttpServletRequest request) {
    return requestService.create(body, request);
  }

  /**
   * 医生修改指定排班的信息
   * @param id 排班ID
   * @param body 修改信息
   */
  @PutMapping("/schedules/{id}")
  public ScheduleRequestResult updateSchedule(
      @PathVariable long id,
      @Valid @RequestBody ScheduleRequestSubmit body,
      HttpServletRequest request) {
    return requestService.update(id, body, request);
  }

  /**
   * 医生删除具体排班
   * @param id 排班ID
   */
  @DeleteMapping("/schedules/{id}")
  public ScheduleRequestResult deleteSchedule(@PathVariable long id, HttpServletRequest request) {
    return requestService.delete(id, request);
  }

  /**
   * 医生查看自己的排班申请。
   */
  @GetMapping("/schedule-requests")
  public java.util.List<ScheduleRequestResult> scheduleRequests(
      @RequestParam(required = false) Integer status, HttpServletRequest request) {
    return requestService.mine(status, request);
  }

  /**
   * 医生查看自己的单条排班申请。
   */
  @GetMapping("/schedule-requests/{id}")
  public ScheduleRequestResult scheduleRequest(
      @PathVariable long id, HttpServletRequest request) {
    return requestService.mine(null, request).stream()
        .filter(x -> x.id() == id)
        .findFirst()
        .orElseThrow(
            () ->
                new io.github.tissyboxc.harmsys.common.UserRegistrationException(
                    404, "排班申请不存在"));
  }

  /**
   * 医生取消自己的待审核排班申请。
   */
  @DeleteMapping("/schedule-requests/{id}")
  public ScheduleRequestResult cancelScheduleRequest(
      @PathVariable long id, HttpServletRequest request) {
    return requestService.cancelMine(id, request);
  }

  /**
   * 获取指定排班的时间段信息
   * @param scheduleId 排班ID
   */
  @GetMapping("/schedules/{scheduleId}/slots")
  public java.util.List<SlotResult> slots(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.slots(scheduleId, request);
  }

  /**
   * 创建指定排班的时间段信息
   * @param scheduleId 排班ID
   * @param body 修改内容
   */
  @PostMapping("/schedules/{scheduleId}/slots")
  public SlotResult createSlot(
      @PathVariable long scheduleId,
      @Valid @RequestBody SlotRequest body,
      HttpServletRequest request) {
    service.slots(scheduleId, request);
    throw new io.github.tissyboxc.harmsys.common.UserRegistrationException(
        409, "医生不能直接新增正式排班时间段，请提交排班修改申请");
  }

  /**
   * 修改指定时间段的状态
   * @param slotId 时间段ID
   * @param body 状态信息
   */
  @PutMapping("/slots/{slotId}/status")
  public void updateSlotStatus(
      @PathVariable long slotId,
      @Valid @RequestBody SlotStatusRequest body,
      HttpServletRequest request) {
    throw new io.github.tissyboxc.harmsys.common.UserRegistrationException(
        409, "医生不能直接修改正式排班时间段状态，请提交排班修改申请");
  }

  /** 医生调整自己时间段状态的请求。 */
  public record SlotStatusRequest(@NotNull @Min(0) @Max(2) Integer status) {}
}


