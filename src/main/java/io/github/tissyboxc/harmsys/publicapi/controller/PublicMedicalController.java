package io.github.tissyboxc.harmsys.publicapi.controller;

import io.github.tissyboxc.harmsys.publicapi.service.PublicMedicalService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
/** 公开医生和科室介绍查询接口。 */
public class PublicMedicalController {
  private final PublicMedicalService service;

  public PublicMedicalController(PublicMedicalService service) {
    this.service = service;
  }

  /**
   * 公开医生信息接口
   * @param department_id 科室ID
   * @param keyword 同时匹配医生真实姓名,编号,擅长领域（LIKE）
   */
  @GetMapping("/doctors")
  public List<Map<String, Object>> doctors(
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) String keyword) {
    return service.doctors(department_id, keyword);
  }

  /**
   * 根据医生ID查询医生信息
   * @param id 医生ID
   */
  @GetMapping("/doctors/{id}")
  public Map<String, Object> doctor(@PathVariable long id) {
    return service.doctor(id);
  }

  /**
   * 查询指定科室的医生信息
   * @param id 科室ID
   */
  @GetMapping("/departments/{id}/doctors")
  public List<Map<String, Object>> departmentDoctors(@PathVariable long id) {
    return doctors(id, null);
  }

  /**
   * 查询所有排班信息
   * @param department_id 科室ID
   * @param doctor_id 医生ID
   * @param schedule_date 排班日期
   * @param period 时间段
   */
  @GetMapping("/schedules")
  public List<Map<String, Object>> schedules(
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) Long doctor_id,
      @RequestParam(required = false) String schedule_date,
      @RequestParam(required = false) Integer period) {
    return service.schedules(department_id, doctor_id, schedule_date, period);
  }

  /**
   * 根据排班ID查询
   * @param id 排班ID
   */
  @GetMapping("/schedules/{id}")
  public Map<String, Object> schedule(@PathVariable long id) {
    return service.schedule(id);
  }

  /**
   * 查询指定排班的时间段
   * @param id 排班ID
   * @return 该排班的时间段信息
   */
  @GetMapping("/schedules/{id}/slots")
  public List<Map<String, Object>> slots(@PathVariable long id) {
    return service.slots(id);
  }
}

