package io.github.tissyboxc.harmsys.pharmacy.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 药房处方查询、发药和库存扣减的数据访问。 */
public interface PharmacyMapper {

  List<Map<String, Object>> selectPrescriptions(
      @Param("status") Integer status, @Param("paymentStatus") Integer paymentStatus);

  Map<String, Object> selectPrescription(@Param("id") long id);

  List<Map<String, Object>> selectPrescriptionItems(@Param("prescriptionId") long prescriptionId);

  Map<String, Object> lockDispensablePrescription(@Param("id") long id);

  List<Map<String, Object>> lockDispenseItems(@Param("prescriptionId") long prescriptionId);

  int deductStock(@Param("medicineId") long medicineId, @Param("quantity") Object quantity);

  int markDispensed(@Param("id") long id);
}
