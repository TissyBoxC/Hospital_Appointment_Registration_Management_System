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

  /**
   * 仅返回已启用的科室信息
   * @param onlyEnabled 可选
   * @return 已启用的所有科室信息
   */
  public List<DepartmentResult> list(boolean onlyEnabled) {
    return repository.findAll(onlyEnabled);
  }

  /**
   * 查询科室创建结果
   * @param id 新科室ID
   */
  public DepartmentResult get(long id) {
    return repository.findById(id).orElseThrow(() -> new UserRegistrationException(404, "科室不存在"));
  }

  /**
   * 管理员新建科室逻辑
   * @param request 新建科室信息
   */
  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult create(DepartmentRequest request, HttpServletRequest httpRequest) {
    //校验用户权限,科室信息
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

  /**
   * 科室信息修改
   * @param id 科室ID
   * @param request 请求体
   */
  @Transactional(rollbackFor = Exception.class)
  public DepartmentResult update(
      long id, DepartmentRequest request, HttpServletRequest httpRequest) {
    //权限校验
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

  /**
   * 科室删除逻辑
   * @param id 科室ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void delete(long id, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireOperator(httpRequest);
    if (!repository.exists(id)) throw new UserRegistrationException(404, "科室不存在");
    //科室下还有子科室或医生,不允删除
    if (repository.hasChildren(id) || repository.hasDoctors(id))
      throw new UserRegistrationException(409, "科室仍有关联子科室或医生，不能删除");
    repository.logicalDelete(id);
    repository.writeLog(
        user.user_id(), "DELETE_DEPARTMENT", id, "删除科室", httpRequest.getRemoteAddr());
  }

  /**
   * 验证父科室是否存在
   * @param parentId 父科室ID
   */
  private void validateParent(Long parentId) {
    if (parentId != null && !repository.exists(parentId))
      throw new UserRegistrationException(422, "父科室不存在");
  }

  /**
   * 权限校验
   */
  private AuthenticatedUser requireOperator(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    authorizationService.requirePermission(request, "DEPARTMENT_MANAGE");
    return user;
  }
}
