package io.github.tissyboxc.harmsys.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.pharmacy.MedicineSearchMatcher;
import io.github.tissyboxc.harmsys.pharmacy.dto.MedicineRequest;
import io.github.tissyboxc.harmsys.pharmacy.dto.StockInRequest;
import io.github.tissyboxc.harmsys.pharmacy.entity.Medicine;
import io.github.tissyboxc.harmsys.pharmacy.mapper.MedicineMapper;
import io.github.tissyboxc.harmsys.pharmacy.service.MedicineService;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 药品库存业务实现。 */
@Service
public class MedicineServiceImpl implements MedicineService {
  private final MedicineMapper medicineMapper;
  private final OperationLogMapper operationLogMapper;

  public MedicineServiceImpl(
      MedicineMapper medicineMapper, OperationLogMapper operationLogMapper) {
    this.medicineMapper = medicineMapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> doctorSearch(AuthenticatedUser user, String keyword) {
    requireDoctor(user);
    LambdaQueryWrapper<Medicine> wrapper =
        new LambdaQueryWrapper<Medicine>()
            .eq(Medicine::getStatus, 1)
            .gt(Medicine::getStockQuantity, BigDecimal.ZERO)
            .orderByAsc(Medicine::getName)
            .orderByAsc(Medicine::getId);
    return match(medicineMapper.selectList(wrapper).stream().map(this::toResult).toList(), keyword);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> pharmacyList(
      AuthenticatedUser user,
      int page,
      int pageSize,
      String keyword,
      Integer status,
      boolean lowStockOnly) {
    requirePharmacy(user);
    if (status != null && status != 0 && status != 1)
      throw new UserRegistrationException(422, "药品状态只能为0或1");

    LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
    if (status != null) wrapper.eq(Medicine::getStatus, status);
    if (lowStockOnly) wrapper.apply("stock_quantity <= warning_quantity");
    wrapper.orderByAsc(Medicine::getName).orderByAsc(Medicine::getId);
    List<Map<String, Object>> matched =
        match(medicineMapper.selectList(wrapper).stream().map(this::toResult).toList(), keyword);

    int currentPage = Math.max(1, page);
    int size = Math.min(Math.max(1, pageSize), 100);
    int offset = (currentPage - 1) * size;
    int fromIndex = Math.min(offset, matched.size());
    int toIndex = Math.min(fromIndex + size, matched.size());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", currentPage);
    result.put("page_size", size);
    result.put("total", matched.size());
    result.put("items", matched.subList(fromIndex, toIndex));
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> detail(AuthenticatedUser user, long id) {
    requirePharmacy(user);
    Medicine medicine = medicineMapper.selectById(id);
    if (medicine == null) throw new UserRegistrationException(404, "药品不存在");
    return toResult(medicine);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(
      AuthenticatedUser user, MedicineRequest body, String ipAddress) {
    requirePharmacy(user);
    Medicine medicine = toEntity(body);
    medicineMapper.insert(medicine);
    writeLog(user.user_id(), "CREATE_MEDICINE", medicine.getId(), "新增药品：" + body.name(), ipAddress);
    return toResult(medicineMapper.selectById(medicine.getId()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      AuthenticatedUser user, long id, MedicineRequest body, String ipAddress) {
    requirePharmacy(user);
    if (medicineMapper.selectById(id) == null)
      throw new UserRegistrationException(404, "药品不存在");
    Medicine medicine = toEntity(body);
    medicine.setId(id);
    medicineMapper.updateById(medicine);
    writeLog(user.user_id(), "UPDATE_MEDICINE", id, "修改药品：" + body.name(), ipAddress);
    return toResult(medicineMapper.selectById(id));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> stockIn(
      AuthenticatedUser user, long id, StockInRequest body, String ipAddress) {
    requirePharmacy(user);
    if (medicineMapper.stockIn(id, body.quantity()) != 1)
      throw new UserRegistrationException(404, "药品不存在");
    writeLog(
        user.user_id(), "STOCK_IN_MEDICINE", id, "药品入库数量：" + body.quantity(), ipAddress);
    return toResult(medicineMapper.selectById(id));
  }

  private Medicine toEntity(MedicineRequest body) {
    Medicine medicine = new Medicine();
    medicine.setMedicineCode(body.medicine_code().trim());
    medicine.setName(body.name().trim());
    medicine.setSpecification(body.specification());
    medicine.setUnit(body.unit());
    medicine.setUnitPrice(body.unit_price());
    medicine.setStockQuantity(body.stock_quantity());
    medicine.setWarningQuantity(body.warning_quantity());
    medicine.setManufacturer(body.manufacturer());
    medicine.setStatus(body.status());
    medicine.setRemark(body.remark());
    return medicine;
  }

  private Map<String, Object> toResult(Medicine medicine) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", medicine.getId());
    result.put("medicine_code", medicine.getMedicineCode());
    result.put("name", medicine.getName());
    result.put("specification", medicine.getSpecification());
    result.put("unit", medicine.getUnit());
    result.put("unit_price", medicine.getUnitPrice());
    result.put("stock_quantity", medicine.getStockQuantity());
    result.put("warning_quantity", medicine.getWarningQuantity());
    result.put("manufacturer", medicine.getManufacturer());
    result.put("status", medicine.getStatus());
    result.put("remark", medicine.getRemark());
    result.put("created_at", medicine.getCreatedAt());
    result.put("updated_at", medicine.getUpdatedAt());
    boolean lowStock =
        medicine.getStockQuantity() != null
            && medicine.getWarningQuantity() != null
            && medicine.getStockQuantity().compareTo(medicine.getWarningQuantity()) <= 0;
    result.put("low_stock", lowStock);
    result.put(
        "stock_text",
        medicine.getStockQuantity() == null
            ? null
            : medicine.getStockQuantity().stripTrailingZeros().toPlainString()
                + " "
                + medicine.getUnit());
    return result;
  }

  private List<Map<String, Object>> match(
      List<Map<String, Object>> candidates, String keyword) {
    if (keyword == null || keyword.isBlank()) return candidates;
    return candidates.stream()
        .map(item -> Map.entry(item, score(item, keyword)))
        .filter(entry -> entry.getValue() > 0)
        .sorted(
            Comparator.<Map.Entry<Map<String, Object>, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(entry -> String.valueOf(entry.getKey().get("name")))
                .thenComparing(entry -> ((Number) entry.getKey().get("id")).longValue()))
        .map(Map.Entry::getKey)
        .toList();
  }

  private int score(Map<String, Object> medicine, String keyword) {
    return MedicineSearchMatcher.score(
        keyword,
        text(medicine, "name"),
        text(medicine, "medicine_code"),
        text(medicine, "specification"),
        text(medicine, "manufacturer"));
  }

  private String text(Map<String, Object> medicine, String key) {
    Object value = medicine.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private void writeLog(
      long userId, String type, long id, String description, String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType("medicine");
    log.setTargetId(id);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private void requireDoctor(AuthenticatedUser user) {
    if (user.doctor_id() == null
        || user.role_codes().stream().noneMatch("DOCTOR"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
  }

  private void requirePharmacy(AuthenticatedUser user) {
    if (user.role_codes().stream()
        .noneMatch(x -> x.equalsIgnoreCase("PHARMACY") || x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "没有药房库存管理权限");
  }
}
