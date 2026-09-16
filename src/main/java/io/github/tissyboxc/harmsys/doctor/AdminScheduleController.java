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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ScheduleResult create(
      @Valid @RequestBody ScheduleRequest body, HttpServletRequest request) {
    return service.createAdminSchedule(body, request);
  }

  @GetMapping
  public java.util.List<ScheduleResult> list(HttpServletRequest request) {
    return service.allSchedules(request);
  }

  @GetMapping("/{scheduleId}")
  public ScheduleResult get(@PathVariable long scheduleId, HttpServletRequest request) {
    return service.adminSchedule(scheduleId, request);
  }

  @PutMapping("/{scheduleId}")
  public ScheduleResult update(
      @PathVariable long scheduleId,
      @Valid @RequestBody ScheduleRequest body,
      HttpServletRequest request) {
    return service.updateAdminSchedule(scheduleId, body, request);
  }

  @GetMapping("/{scheduleId}/slots")
  public java.util.List<SlotResult> slots(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.adminSlots(scheduleId, request);
  }

  @PostMapping("/{scheduleId}/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public SlotResult createSlot(
      @PathVariable long scheduleId,
      @Valid @RequestBody SlotRequest body,
      HttpServletRequest request) {
    return service.createAdminSlot(scheduleId, body, request);
  }

  @PutMapping("/slots/{slotId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSlotStatus(
      @PathVariable long slotId,
      @Valid @RequestBody AdminSlotStatusRequest body,
      HttpServletRequest request) {
    service.adminUpdateSlotStatus(slotId, body.status(), request);
  }

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
