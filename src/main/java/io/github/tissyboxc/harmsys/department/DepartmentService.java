package io.github.tissyboxc.harmsys.department;

import io.github.tissyboxc.harmsys.department.dto.*;
import io.github.tissyboxc.harmsys.users.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 科室增删改查和启停状态管理的业务服务。 */
public class DepartmentService {
  private final DepartmentRepository repository;
  private final PermissionAuthorizationService authorizationService;

  public DepartmentService(
      DepartmentRepository repository, PermissionAuthorizationService authorizationService) {
    this.repository = repository;
    this.authorizationService = authorizationService;
  }

  public List<DepartmentResult> list(boolean onlyEnabled) {
    return repository.findAll(onlyEnabled);
  }

  public DepartmentResult get(long id) {
    return repository.findById(id).orElseThrow(() -> new UserRegistrationException(404, "科室不存在"));
  }

  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult create(DepartmentRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    validateParent(request.parent_id());
    try {
      long id = repository.insert(request);
      repository.writeLog(
          user.user_id(), "CREATE_DEPARTMENT", id, "创建科室", httpRequest.getRemoteAddr());
      return get(id);
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "科室编码或同级科室名称已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult update(
      long id, DepartmentRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    if (request.parent_id() != null && request.parent_id() == id)
      throw new UserRegistrationException(422, "科室不能将自己设置为父科室");
    validateParent(request.parent_id());
    try {
      repository.update(id, request);
      repository.writeLog(
          user.user_id(), "UPDATE_DEPARTMENT", id, "修改科室", httpRequest.getRemoteAddr());
      return get(id);
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "科室编码或同级科室名称已存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(long id, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    if (!repository.exists(id)) throw new UserRegistrationException(404, "科室不存在");
    if (repository.hasChildren(id) || repository.hasDoctors(id))
      throw new UserRegistrationException(409, "科室仍有关联子科室或医生，不能删除");
    repository.logicalDelete(id);
    repository.writeLog(
        user.user_id(), "DELETE_DEPARTMENT", id, "删除科室", httpRequest.getRemoteAddr());
  }

  private void validateParent(Long parentId) {
    if (parentId != null && !repository.exists(parentId))
      throw new UserRegistrationException(422, "父科室不存在");
  }

  private AuthenticatedUser requireOperator(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    authorizationService.requirePermission(request, "DEPARTMENT_MANAGE");
    return user;
  }
}
