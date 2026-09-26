package io.github.tissyboxc.harmsys.publicapi.controller;

import io.github.tissyboxc.harmsys.publicapi.service.PublicSchedulePageService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** 公共排班分页查询，返回有剩余号源的可预约排班。 */
@RestController
@RequestMapping("/api/public/schedules")
/** 公开排班分页查询接口。 */
public class PublicSchedulePageController {
  private final PublicSchedulePageService service;

  public PublicSchedulePageController(PublicSchedulePageService service) {
    this.service = service;
  }

  /**
   * 排班信息分页查询
   * @param department_id 科室ID
   * @param doctor_id 医生ID
   */
  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) Long doctor_id) {
    return service.page(page, page_size, department_id, doctor_id);
  }
}

