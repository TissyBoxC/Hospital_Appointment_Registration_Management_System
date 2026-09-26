package io.github.tissyboxc.harmsys.operationlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.operationlog.service.OperationLogService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 操作日志查询业务实现。 */
@Service
public class OperationLogServiceImpl implements OperationLogService {
  private final OperationLogMapper operationLogMapper;

  public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(
      AuthenticatedUser user,
      Long userId,
      String operationType,
      String targetType,
      Long targetId,
      String startTime,
      String endTime,
      int limit) {
    requireAdmin(user);
    LambdaQueryWrapper<OperationLog> wrapper =
        buildWrapper(userId, operationType, targetType, targetId, startTime, endTime);
    wrapper.orderByDesc(OperationLog::getId).last("LIMIT " + Math.max(1, Math.min(limit, 500)));
    return operationLogMapper.selectList(wrapper).stream().map(this::toResult).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> get(AuthenticatedUser user, long id) {
    requireAdmin(user);
    OperationLog log = operationLogMapper.selectById(id);
    if (log == null) throw new UserRegistrationException(404, "操作日志不存在");
    return toResult(log);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> paged(AuthenticatedUser user, int page, int pageSize) {
    requireAdmin(user);
    Page<OperationLog> result =
        operationLogMapper.selectPage(
            Page.of(Math.max(1, page), Math.max(1, Math.min(pageSize, 100))),
            new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId));
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("page", result.getCurrent());
    response.put("page_size", result.getSize());
    response.put("total", result.getTotal());
    response.put("items", result.getRecords().stream().map(this::toResult).toList());
    return response;
  }

  private LambdaQueryWrapper<OperationLog> buildWrapper(
      Long userId,
      String operationType,
      String targetType,
      Long targetId,
      String startTime,
      String endTime) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(userId != null, OperationLog::getUserId, userId);
    if (operationType != null && !operationType.isBlank())
      wrapper.eq(OperationLog::getOperationType, operationType.trim());
    if (targetType != null && !targetType.isBlank())
      wrapper.eq(OperationLog::getTargetType, targetType.trim());
    wrapper.eq(targetId != null, OperationLog::getTargetId, targetId);
    if (startTime != null && !startTime.isBlank())
      wrapper.ge(OperationLog::getCreatedAt, startTime);
    if (endTime != null && !endTime.isBlank())
      wrapper.le(OperationLog::getCreatedAt, endTime);
    return wrapper;
  }

  private Map<String, Object> toResult(OperationLog log) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", log.getId());
    result.put("user_id", log.getUserId());
    result.put("operation_type", log.getOperationType());
    result.put("target_type", log.getTargetType());
    result.put("target_id", log.getTargetId());
    result.put("description", log.getDescription());
    result.put("ip_address", log.getIpAddress());
    result.put("created_at", log.getCreatedAt());
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以查询操作日志");
  }
}
