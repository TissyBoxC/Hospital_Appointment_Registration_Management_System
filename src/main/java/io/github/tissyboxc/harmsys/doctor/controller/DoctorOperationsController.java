package io.github.tissyboxc.harmsys.doctor.controller;

import io.github.tissyboxc.harmsys.doctor.service.DoctorOperationsService;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
/** 医生患者历史查询及时间段完整维护。 */
public class DoctorOperationsController {
  private final DoctorOperationsService service;

  public DoctorOperationsController(DoctorOperationsService service) {
    this.service = service;
  }

  /**
   * 医生获取自己的患者
   * @param keyword 匹配患者ID,真实姓名，电话
   */
  @GetMapping("/patients")
  public List<Map<String, Object>> patients(
      @RequestParam(required = false) String keyword, HttpServletRequest request) {
    return service.patients(SessionAuth.require(request), keyword);
  }

  /**
   * 医生获取指定患者的信息
   * @param id 患者ID
   */
  @GetMapping("/patients/{id}")
  public Map<String, Object> patient(@PathVariable long id, HttpServletRequest request) {
    return service.patient(SessionAuth.require(request), id);
  }

  /**
   * 获取指定患者在自己这里的历史就诊信息
   * @param id 患者iD
   */
  @GetMapping("/patients/{id}/history")
  public Map<String, Object> history(@PathVariable long id, HttpServletRequest request) {
    return service.history(SessionAuth.require(request), id);
  }

  /**
   * 医生不能直接修改正式排班的时间段，必须提交排班修改申请。
   */
  @PutMapping("/slots/{slotId}")
  public Map<String, Object> updateSlot(
      @PathVariable long slotId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    return service.rejectDirectSlotUpdate(SessionAuth.require(request));
  }

  /**
   * 医生不能直接删除正式排班的时间段，必须提交排班修改或删除申请。
   */
  @DeleteMapping("/slots/{slotId}")
  public void deleteSlot(@PathVariable long slotId, HttpServletRequest request) {
    service.rejectDirectSlotDelete(SessionAuth.require(request));
  }
}


