package io.github.tissyboxc.harmsys.pharmacy.service.impl;

import io.github.tissyboxc.harmsys.pharmacy.service.PharmacyService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.pharmacy.mapper.PharmacyMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 药房处方查询和发药业务逻辑。 */
@Service
public class PharmacyServiceImpl implements PharmacyService {
  private final PharmacyMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public PharmacyServiceImpl(PharmacyMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> prescriptions(
      AuthenticatedUser operator, Integer status, Integer paymentStatus) {
    requirePharmacy(operator);
    if (status != null && (status < 1 || status > 3))
      throw new UserRegistrationException(422, "处方状态只能为1到3");
    if (paymentStatus != null && (paymentStatus < 1 || paymentStatus > 3))
      throw new UserRegistrationException(422, "处方支付状态只能为1到3");
    return mapper.selectPrescriptions(status, paymentStatus);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> prescription(AuthenticatedUser operator, long id) {
    requirePharmacy(operator);
    Map<String, Object> result = mapper.selectPrescription(id);
    if (result == null) throw new UserRegistrationException(404, "处方不存在");
    result.put("items", mapper.selectPrescriptionItems(id));
    return result;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void dispense(AuthenticatedUser operator, long id, String ipAddress) {
    requirePharmacy(operator);
    Map<String, Object> prescription = mapper.lockDispensablePrescription(id);
    if (prescription == null)
      throw new UserRegistrationException(409, "处方不存在、未支付或当前不能标记为已取药");
    List<Map<String, Object>> items = mapper.lockDispenseItems(id);
    if (items.isEmpty()) throw new UserRegistrationException(409, "处方没有药品明细，不能发药");
    for (Map<String, Object> item : items) {
      if (item.get("medicine_id") == null)
        throw new UserRegistrationException(409, "处方包含未关联库存药品的明细，不能发药");
      BigDecimal required = (BigDecimal) item.get("quantity");
      BigDecimal stock = (BigDecimal) item.get("stock_quantity");
      if (stock == null || stock.compareTo(required) < 0)
        throw new UserRegistrationException(
            409,
            "药品“"
                + item.get("drug_name")
                + "”库存不足。现有 "
                + (stock == null ? "0" : stock)
                + "，需要 "
                + required);
    }
    for (Map<String, Object> item : items) {
      if (mapper.deductStock(
              ((Number) item.get("medicine_id")).longValue(), item.get("quantity"))
          != 1)
        throw new UserRegistrationException(
            409, "药品“" + item.get("drug_name") + "”库存不足，发药失败");
    }
    mapper.markDispensed(id);
    OperationLog log = new OperationLog();
    log.setUserId(operator.user_id());
    log.setOperationType("DISPENSE_PRESCRIPTION");
    log.setTargetType("prescription");
    log.setTargetId(id);
    log.setDescription("药房确认患者已取药");
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private void requirePharmacy(AuthenticatedUser user) {
    if (user.role_codes().stream()
        .noneMatch(role -> role.equalsIgnoreCase("PHARMACY") || role.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "没有药房操作权限");
  }
}
