package io.github.tissyboxc.harmsys.pharmacy.service;

import io.github.tissyboxc.harmsys.pharmacy.MedicineSearchMatcher;
import io.github.tissyboxc.harmsys.pharmacy.dto.MedicineRequest;
import io.github.tissyboxc.harmsys.pharmacy.dto.StockInRequest;
import io.github.tissyboxc.harmsys.pharmacy.repository.MedicineRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 药品库存业务逻辑。 */
@Service
public class MedicineService {
  private final MedicineRepository repository;

  public MedicineService(MedicineRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> doctorSearch(AuthenticatedUser user, String keyword) {
    requireDoctor(user);
    return match(repository.searchable(), keyword);
  }

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
    int currentPage = Math.max(1, page);
    int size = Math.min(Math.max(1, pageSize), 100);
    int offset = (currentPage - 1) * size;
    List<Map<String, Object>> matched =
        match(repository.candidates(status, lowStockOnly), keyword);
    int fromIndex = Math.min(offset, matched.size());
    int toIndex = Math.min(fromIndex + size, matched.size());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("page", currentPage);
    result.put("page_size", size);
    result.put("total", matched.size());
    result.put("items", matched.subList(fromIndex, toIndex));
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(AuthenticatedUser user, long id) {
    requirePharmacy(user);
    Map<String, Object> medicine = repository.findById(id);
    if (medicine == null) throw new UserRegistrationException(404, "药品不存在");
    return medicine;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(
      AuthenticatedUser user, MedicineRequest body, String ipAddress) {
    requirePharmacy(user);
    long id = repository.insert(body);
    repository.insertOperationLog(
        user.user_id(), "CREATE_MEDICINE", id, "新增药品：" + body.name(), ipAddress);
    return repository.findById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      AuthenticatedUser user, long id, MedicineRequest body, String ipAddress) {
    requirePharmacy(user);
    if (repository.update(id, body) != 1)
      throw new UserRegistrationException(404, "药品不存在");
    repository.insertOperationLog(
        user.user_id(), "UPDATE_MEDICINE", id, "修改药品：" + body.name(), ipAddress);
    return repository.findById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> stockIn(
      AuthenticatedUser user, long id, StockInRequest body, String ipAddress) {
    requirePharmacy(user);
    if (repository.stockIn(id, body.quantity()) != 1)
      throw new UserRegistrationException(404, "药品不存在");
    repository.insertOperationLog(
        user.user_id(), "STOCK_IN_MEDICINE", id, "药品入库数量：" + body.quantity(), ipAddress);
    return repository.findById(id);
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

