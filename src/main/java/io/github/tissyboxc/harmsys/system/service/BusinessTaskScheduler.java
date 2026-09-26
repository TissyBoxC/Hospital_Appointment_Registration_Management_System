package io.github.tissyboxc.harmsys.system.service;

import io.github.tissyboxc.harmsys.config.database.DatabaseInitializationState;
import io.github.tissyboxc.harmsys.system.repository.BusinessMaintenanceRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 周期性维护预约、排班和号源状态。 */
@Component
public class BusinessTaskScheduler {
  private static final Logger log = LoggerFactory.getLogger(BusinessTaskScheduler.class);
  private final BusinessMaintenanceRepository repository;

  // 数据库初始化完成前不访问业务表。
  private final DatabaseInitializationState initializationState;

  public BusinessTaskScheduler(
      BusinessMaintenanceRepository repository, DatabaseInitializationState initializationState) {
    this.repository = repository;
    this.initializationState = initializationState;
  }

  @Scheduled(fixedDelayString = "${harms.jobs.interval-ms:60000}") // 60秒一次
  @Transactional(rollbackFor = Exception.class)
  public void maintainBusinessStatus() {
    if (!initializationState.isInitialized()) {
      log.debug("数据库初始化尚未完成，跳过本次业务维护任务");
      return;
    }
    repository.activateDueSchedules();
    repository.closeExpiredSchedules();
    List<Map<String, Object>> expired = repository.findExpiredAppointments();
    for (Map<String, Object> appointment : expired) {
      long appointmentId = ((Number) appointment.get("id")).longValue();
      if (!repository.expireAppointment(appointmentId)) continue;
      if (appointment.get("slot_id") != null)
        repository.releaseSlot(((Number) appointment.get("slot_id")).longValue());
      repository.decrementBookedCount(((Number) appointment.get("schedule_id")).longValue());
    }
    repository.markNoShows();
    repository.reconcileBookedCounts();
    repository.repairOrphanSlots();
    repository.reconcilePayments();
  }
}
