package io.github.tissyboxc.harmsys.department.service;

import io.github.tissyboxc.harmsys.department.dto.DepartmentRequest;
import io.github.tissyboxc.harmsys.department.dto.DepartmentResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/** 科室增删改查和启停状态管理业务。 */
public interface DepartmentService {

  List<DepartmentResult> list(boolean onlyEnabled);

  DepartmentResult get(long id);

  DepartmentResult create(DepartmentRequest request, HttpServletRequest httpRequest);

  DepartmentResult update(long id, DepartmentRequest request, HttpServletRequest httpRequest);

  void delete(long id, HttpServletRequest httpRequest);
}
