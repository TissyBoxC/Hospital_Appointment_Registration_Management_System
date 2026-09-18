package io.github.tissyboxc.harmsys.department;

import io.github.tissyboxc.harmsys.department.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
/** 管理员维护科室信息的接口。 */
public class DepartmentController {
  private final DepartmentService service;

  public DepartmentController(DepartmentService service) {
    this.service = service;
  }

  /**
   * 科室创建接口
   * @param request 新建科室信息
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DepartmentResult create(
      @Valid @RequestBody DepartmentRequest request, HttpServletRequest httpRequest) {
    return service.create(request, httpRequest);
  }

  /**
   * 科室信息修改
   * @param id 科室ID
   * @param request 请求体包含修改内容
   */
  @PutMapping("/{id}")
  public DepartmentResult update(
      @PathVariable long id,
      @Valid @RequestBody DepartmentRequest request,
      HttpServletRequest httpRequest) {
    return service.update(id, request, httpRequest);
  }

  /**
   * 删除科室
   * @param id 科室ID
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id, HttpServletRequest httpRequest) {
    service.delete(id, httpRequest);
  }

  /**
   * 查询科室信息
   * @return 所有课室,包含未启用的
   */
  @GetMapping
  public java.util.List<DepartmentResult> list() {
    return service.list(false);
  }

  /**
   * 根据ID获取科室信息
   * @param id 科室ID
   */
  @GetMapping("/{id}")
  public DepartmentResult get(@PathVariable long id) {
    return service.get(id);
  }
}
