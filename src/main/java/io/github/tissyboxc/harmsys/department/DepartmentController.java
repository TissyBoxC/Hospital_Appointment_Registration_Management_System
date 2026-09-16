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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DepartmentResult create(
      @Valid @RequestBody DepartmentRequest request, HttpServletRequest httpRequest) {
    return service.create(request, httpRequest);
  }

  @PutMapping("/{id}")
  public DepartmentResult update(
      @PathVariable long id,
      @Valid @RequestBody DepartmentRequest request,
      HttpServletRequest httpRequest) {
    return service.update(id, request, httpRequest);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id, HttpServletRequest httpRequest) {
    service.delete(id, httpRequest);
  }

  @GetMapping
  public java.util.List<DepartmentResult> list() {
    return service.list(false);
  }

  @GetMapping("/{id}")
  public DepartmentResult get(@PathVariable long id) {
    return service.get(id);
  }
}
