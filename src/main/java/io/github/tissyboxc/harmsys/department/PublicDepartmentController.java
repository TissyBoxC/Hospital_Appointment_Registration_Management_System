package io.github.tissyboxc.harmsys.department;

import io.github.tissyboxc.harmsys.department.dto.DepartmentResult;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/departments")
/** 无需登录即可查询启用科室的接口。 */
public class PublicDepartmentController {
  private final DepartmentService service;

  public PublicDepartmentController(DepartmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<DepartmentResult> list() {
    return service.list(true);
  }

  @GetMapping("/{id}")
  public DepartmentResult get(@PathVariable long id) {
    return service.get(id);
  }
}
