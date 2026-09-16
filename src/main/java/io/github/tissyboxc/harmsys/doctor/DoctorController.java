package io.github.tissyboxc.harmsys.doctor;

import io.github.tissyboxc.harmsys.doctor.dto.*;
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

  public DoctorController(DoctorService service) {
    this.service = service;
  }

  @GetMapping("/profile")
  public DoctorProfileResult profile(HttpServletRequest request) {
    return service.profile(request);
  }

  @PutMapping("/profile")
  public DoctorProfileResult update(
      @Valid @RequestBody DoctorProfileUpdateRequest body, HttpServletRequest request) {
    return service.updateProfile(body, request);
  }

  @GetMapping("/schedules")
  public java.util.List<ScheduleResult> schedules(HttpServletRequest request) {
    return service.schedules(request);
  }

  @GetMapping("/schedules/{scheduleId}")
  public ScheduleResult schedule(@PathVariable long scheduleId, HttpServletRequest request) {
    return service.scheduleForDoctor(scheduleId, request);
  }

  @PostMapping("/schedules")
  @ResponseStatus(HttpStatus.CREATED)
  public ScheduleResult createSchedule(
      @Valid @RequestBody ScheduleRequest body, HttpServletRequest request) {
    return service.createOwnSchedule(body, request);
  }

  @PutMapping("/schedules/{id}")
  public ScheduleResult updateSchedule(
      @PathVariable long id, @Valid @RequestBody ScheduleRequest body, HttpServletRequest request) {
    return service.updateOwnSchedule(id, body, request);
  }

  @DeleteMapping("/schedules/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSchedule(@PathVariable long id, HttpServletRequest request) {
    service.deleteOwnSchedule(id, request);
  }

  @GetMapping("/schedules/{scheduleId}/slots")
  public java.util.List<SlotResult> slots(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.slots(scheduleId, request);
  }

  @PostMapping("/schedules/{scheduleId}/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public SlotResult createSlot(
      @PathVariable long scheduleId,
      @Valid @RequestBody SlotRequest body,
      HttpServletRequest request) {
    return service.createOwnSlot(scheduleId, body, request);
  }

  @PutMapping("/slots/{slotId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSlotStatus(
      @PathVariable long slotId,
      @Valid @RequestBody SlotStatusRequest body,
      HttpServletRequest request) {
    service.updateSlotStatus(slotId, body.status(), request);
  }

  /** 医生调整自己时间段状态的请求。 */
  public record SlotStatusRequest(@NotNull @Min(0) @Max(2) Integer status) {}
}
