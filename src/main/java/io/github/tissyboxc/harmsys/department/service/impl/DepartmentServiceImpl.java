package io.github.tissyboxc.harmsys.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.department.dto.DepartmentRequest;
import io.github.tissyboxc.harmsys.department.dto.DepartmentResult;
import io.github.tissyboxc.harmsys.department.entity.Department;
import io.github.tissyboxc.harmsys.department.mapper.DepartmentMapper;
import io.github.tissyboxc.harmsys.department.service.DepartmentService;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 科室业务实现。 */
@Service
public class DepartmentServiceImpl implements DepartmentService {
  private final DepartmentMapper departmentMapper;
  private final OperationLogMapper operationLogMapper;
  private final PermissionAuthorizationService authorizationService;

  public DepartmentServiceImpl(
      DepartmentMapper departmentMapper,
      OperationLogMapper operationLogMapper,
      PermissionAuthorizationService authorizationService) {
    this.departmentMapper = departmentMapper;
    this.operationLogMapper = operationLogMapper;
    this.authorizationService = authorizationService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DepartmentResult> list(boolean onlyEnabled) {
    LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
    if (onlyEnabled) wrapper.eq(Department::getStatus, 1);
    wrapper.orderByAsc(Department::getSortNo).orderByAsc(Department::getId);
    return departmentMapper.selectList(wrapper).stream().map(this::toResult).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DepartmentResult get(long id) {
    Department department = departmentMapper.selectById(id);
    if (department == null) throw new UserRegistrationException(404, "科室不存在");
    return toResult(department);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult create(DepartmentRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    validateParent(request.parent_id());
    try {
      Department department = toEntity(request);
      departmentMapper.insert(department);
      writeLog(
          user.user_id(), "CREATE_DEPARTMENT", department.getId(), "创建科室", httpRequest);
      return get(department.getId());
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "科室编码或同级科室名称已存在");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult update(
      long id, DepartmentRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    if (request.parent_id() != null && request.parent_id() == id)
      throw new UserRegistrationException(422, "科室不能将自己设置为父科室");
    validateParent(request.parent_id());
    try {
      Department current = departmentMapper.selectById(id);
      if (current == null) throw new UserRegistrationException(404, "科室不存在或已删除");
      Department department = toEntity(request);
      department.setId(id);
      departmentMapper.updateById(department);
      writeLog(user.user_id(), "UPDATE_DEPARTMENT", id, "修改科室", httpRequest);
      return get(id);
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "科室编码或同级科室名称已存在");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(long id, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    if (departmentMapper.selectById(id) == null)
      throw new UserRegistrationException(404, "科室不存在");
    Long children =
        departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
    if (children != null && children > 0)
      throw new UserRegistrationException(409, "科室仍有关联子科室或医生，不能删除");

    Long doctors = departmentMapper.countDoctors(id);
    if (doctors != null && doctors > 0)
      throw new UserRegistrationException(409, "科室仍有关联子科室或医生，不能删除");
    departmentMapper.deleteById(id);
    writeLog(user.user_id(), "DELETE_DEPARTMENT", id, "删除科室", httpRequest);
  }

  private void validateParent(Long parentId) {
    if (parentId != null && departmentMapper.selectById(parentId) == null)
      throw new UserRegistrationException(422, "父科室不存在");
  }

  private AuthenticatedUser requireOperator(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    authorizationService.requirePermission(request, "DEPARTMENT_MANAGE");
    return user;
  }

  private void writeLog(
      long userId, String operationType, long targetId, String description,
      HttpServletRequest request) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(operationType);
    log.setTargetType("department");
    log.setTargetId(targetId);
    log.setDescription(description);
    log.setIpAddress(request.getRemoteAddr());
    operationLogMapper.insert(log);
  }

  private Department toEntity(DepartmentRequest request) {
    Department department = new Department();
    department.setParentId(request.parent_id());
    department.setName(request.name());
    department.setCode(request.code());
    department.setDescription(request.description());
    department.setLocation(request.location());
    department.setContactPhone(request.contact_phone());
    department.setSortNo(request.sort_no());
    department.setStatus(request.status());
    return department;
  }

  private DepartmentResult toResult(Department department) {
    return new DepartmentResult(
        department.getId(),
        department.getParentId(),
        department.getName(),
        department.getCode(),
        department.getDescription(),
        department.getLocation(),
        department.getContactPhone(),
        department.getSortNo(),
        department.getStatus(),
        department.getCreatedAt(),
        department.getUpdatedAt());
  }
}
