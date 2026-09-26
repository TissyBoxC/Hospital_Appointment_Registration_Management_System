package io.github.tissyboxc.harmsys.department.controller;

import io.github.tissyboxc.harmsys.department.dto.DepartmentDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.department.service.DepartmentManagerService;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import io.github.tissyboxc.harmsys.doctor.dto.SlotRequest;
import io.github.tissyboxc.harmsys.doctor.dto.SlotResult;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拥有科室管理权限的用户查看和维护本人科室的医生、排班及就诊患者。
 *
 * <p>所有接口都复用 {@code /api/departments/**} 的 DEPARTMENT_MANAGE 权限，并由服务层再次校验
 * 医生的所属科室，防止通过 department_id、doctorId 或 scheduleId 越权访问其他科室。
 */
@RestController
@RequestMapping("/api/departments/manager")
public class DepartmentManagerController {
  private final DepartmentManagerService service;

  public DepartmentManagerController(DepartmentManagerService service) {
    this.service = service;
  }

  /** 当前负责人管理的科室范围。 */
  @GetMapping("/scope")
  public Map<String, Object> scope(HttpServletRequest request) {
    return service.scope(SessionAuth.require(request));
  }

  /** 查询负责科室下的医生，可按姓名、工号、职称或擅长领域检索。 */
  @GetMapping("/doctors")
  public List<Map<String, Object>> doctors(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    return service.doctors(SessionAuth.require(request), keyword);
  }

  /** 查看负责科室下指定医生的完整资料。 */
  @GetMapping("/doctors/{doctorId}")
  public Map<String, Object> doctor(
      @PathVariable long doctorId, HttpServletRequest request) {
    return service.doctor(SessionAuth.require(request), doctorId);
  }

  /** 修改负责科室下指定医生的资料和执业状态。 */
  @PutMapping("/doctors/{doctorId}")
  public Map<String, Object> updateDoctor(
      @PathVariable long doctorId,
      @Valid @RequestBody DepartmentDoctorUpdateRequest body,
      HttpServletRequest request) {
    return service.updateDoctor(
        SessionAuth.require(request), doctorId, body, request.getRemoteAddr());
  }

  /** 查询负责科室下的排班，可按医生和日期检索。 */
  @GetMapping("/schedules")
  public List<ScheduleResult> schedules(
      @RequestParam(required = false) Long doctor_id,
      @RequestParam(required = false) java.time.LocalDate schedule_date,
      HttpServletRequest request) {
    return service.schedules(SessionAuth.require(request), doctor_id, schedule_date);
  }

  /** 查看负责科室下指定排班。 */
  @GetMapping("/schedules/{scheduleId}")
  public ScheduleResult schedule(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.schedule(SessionAuth.require(request), scheduleId);
  }

  /** 在负责科室下创建医生排班。 */
  @PostMapping("/schedules")
  @ResponseStatus(HttpStatus.CREATED)
  public ScheduleResult createSchedule(
      @Valid @RequestBody ScheduleRequest body, HttpServletRequest request) {
    return service.createSchedule(SessionAuth.require(request), body, request.getRemoteAddr());
  }

  /** 修改负责科室下尚未产生预约的排班。 */
  @PutMapping("/schedules/{scheduleId}")
  public ScheduleResult updateSchedule(
      @PathVariable long scheduleId,
      @Valid @RequestBody ScheduleRequest body,
      HttpServletRequest request) {
    return service.updateSchedule(
        SessionAuth.require(request), scheduleId, body, request.getRemoteAddr());
  }

  /** 删除负责科室下尚未产生预约的排班。 */
  @DeleteMapping("/schedules/{scheduleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSchedule(
      @PathVariable long scheduleId, HttpServletRequest request) {
    service.deleteSchedule(SessionAuth.require(request), scheduleId, request.getRemoteAddr());
  }

  /** 查询指定排班的时间段。 */
  @GetMapping("/schedules/{scheduleId}/slots")
  public List<SlotResult> slots(
      @PathVariable long scheduleId, HttpServletRequest request) {
    return service.slots(SessionAuth.require(request), scheduleId);
  }

  /** 为负责科室下的排班创建时间段。 */
  @PostMapping("/schedules/{scheduleId}/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public SlotResult createSlot(
      @PathVariable long scheduleId,
      @Valid @RequestBody SlotRequest body,
      HttpServletRequest request) {
    return service.createSlot(
        SessionAuth.require(request), scheduleId, body, request.getRemoteAddr());
  }

  /** 修改负责科室下排班时间段的状态。 */
  @PutMapping("/slots/{slotId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSlotStatus(
      @PathVariable long slotId,
      @Valid @RequestBody DepartmentSlotStatusRequest body,
      HttpServletRequest request) {
    service.updateSlotStatus(
        SessionAuth.require(request), slotId, body.status(), request.getRemoteAddr());
  }

  /**
   * 查询在负责科室就诊过的患者。
   *
   * <p>只返回与负责科室的预约或就诊有关联的患者，不返回全院患者。
   */
  @GetMapping("/patients")
  public List<Map<String, Object>> patients(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    return service.patients(SessionAuth.require(request), keyword);
  }

  /** 查看负责科室患者的资料及在该科室下的预约、就诊记录。 */
  @GetMapping("/patients/{patientId}")
  public Map<String, Object> patient(
      @PathVariable long patientId, HttpServletRequest request) {
    return service.patient(SessionAuth.require(request), patientId);
  }

  /** 科室负责人调整排班时间段状态的请求。 */
  public record DepartmentSlotStatusRequest(
      @NotNull @Min(0) @Max(2) Integer status) {}
}
