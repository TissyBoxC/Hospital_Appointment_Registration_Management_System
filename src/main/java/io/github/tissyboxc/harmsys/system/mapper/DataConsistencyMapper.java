package io.github.tissyboxc.harmsys.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 业务数据一致性检查和修复的数据访问。 */
public interface DataConsistencyMapper {

  long countScheduleBookedCountMismatch();

  long countOrphanReservedSlots();

  long countPaymentStatusMismatch();

  long countOrphanSlots();

  int repairBookedCounts();

  int repairOrphanReservedSlots();

  int repairPayments();
}
