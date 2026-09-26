package io.github.tissyboxc.harmsys.pharmacy.service;

import io.github.tissyboxc.harmsys.pharmacy.dto.MedicineRequest;
import io.github.tissyboxc.harmsys.pharmacy.dto.StockInRequest;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 药品库存业务。 */
public interface MedicineService {

  List<Map<String, Object>> doctorSearch(AuthenticatedUser user, String keyword);

  Map<String, Object> pharmacyList(
      AuthenticatedUser user,
      int page,
      int pageSize,
      String keyword,
      Integer status,
      boolean lowStockOnly);

  Map<String, Object> detail(AuthenticatedUser user, long id);

  Map<String, Object> create(
      AuthenticatedUser user, MedicineRequest body, String ipAddress);

  Map<String, Object> update(
      AuthenticatedUser user, long id, MedicineRequest body, String ipAddress);

  Map<String, Object> stockIn(
      AuthenticatedUser user, long id, StockInRequest body, String ipAddress);
}
