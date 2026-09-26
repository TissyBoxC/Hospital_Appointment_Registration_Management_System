package io.github.tissyboxc.harmsys.system.service.impl;

import io.github.tissyboxc.harmsys.system.service.DataConsistencyService;

import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.system.mapper.DataConsistencyMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业务数据一致性检查和修复业务逻辑。 */
@Service
public class DataConsistencyServiceImpl implements DataConsistencyService {
  private final DataConsistencyMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public DataConsistencyServiceImpl(
      DataConsistencyMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> check(AuthenticatedUser user) {
    requireAdmin(user);
    return checkResult();
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> repair(AuthenticatedUser user) {
    requireAdmin(user);
    int affected =
        mapper.repairBookedCounts()
            + mapper.repairOrphanReservedSlots()
            + mapper.repairPayments();
    OperationLog log = new OperationLog();
    log.setUserId(user.user_id());
    log.setOperationType("REPAIR_DATA_CONSISTENCY");
    log.setTargetType("system");
    log.setDescription("管理员执行数据一致性修复，共影响" + affected + "条记录");
    operationLogMapper.insert(log);
    Map<String, Object> result = checkResult();
    result.put("affected_rows", affected);
    return result;
  }

  private Map<String, Object> checkResult() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "schedule_booked_count_mismatch", mapper.countScheduleBookedCountMismatch());
    result.put("orphan_reserved_slots", mapper.countOrphanReservedSlots());
    result.put("payment_status_mismatch", mapper.countPaymentStatusMismatch());
    result.put("orphan_slots", mapper.countOrphanSlots());
    return result;
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行数据一致性检查");
  }
}
