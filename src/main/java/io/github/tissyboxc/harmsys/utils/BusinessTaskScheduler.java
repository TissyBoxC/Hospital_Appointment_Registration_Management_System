package io.github.tissyboxc.harmsys.utils;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 周期性维护预约、排班和号源状态。 */
@Component
/** 定时处理超时预约、通知外发等后台业务任务。 */
public class BusinessTaskScheduler {
  private static final Logger log = LoggerFactory.getLogger(BusinessTaskScheduler.class);
  private final JdbcTemplate jdbc;
  private final DatabaseInitializationState initializationState;

  public BusinessTaskScheduler(JdbcTemplate jdbc, DatabaseInitializationState initializationState) {
    this.jdbc = jdbc;
    this.initializationState = initializationState;
  }

  @Scheduled(fixedDelayString = "${harms.jobs.interval-ms:60000}")
  @Transactional(rollbackFor = Exception.class)
  public void maintainBusinessStatus() {
    if (!initializationState.isInitialized()) {
      log.debug("数据库初始化尚未完成，跳过本次业务维护任务");
      return;
    }
    jdbc.update(
        "UPDATE doctor_schedule SET status=1 WHERE status=0 AND schedule_date=CURRENT_DATE AND"
            + " start_time>CURTIME() AND EXISTS (SELECT 1 FROM doctor d WHERE"
            + " d.id=doctor_schedule.doctor_id AND d.status=1 AND d.deleted=0)");
    jdbc.update(
        "UPDATE doctor_schedule SET status=3 WHERE status=1 AND (schedule_date<CURRENT_DATE OR"
            + " (schedule_date=CURRENT_DATE AND end_time<=CURTIME()))");
    List<Map<String, Object>> expired =
        jdbc.queryForList(
            "SELECT id,schedule_id,slot_id FROM appointment WHERE status IN (1,2) AND"
                + " ((appointment_date<CURRENT_DATE) OR (appointment_date=CURRENT_DATE AND"
                + " created_at < DATE_SUB(NOW(), INTERVAL 1 DAY)))");
    for (Map<String, Object> a : expired) {
      long id = ((Number) a.get("id")).longValue();
      if (jdbc.update(
              "UPDATE appointment SET status=8,cancel_reason='系统自动过期' WHERE id=? AND status IN"
                  + " (1,2)",
              id)
          == 1) {
        if (a.get("slot_id") != null)
          jdbc.update(
              "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1",
              ((Number) a.get("slot_id")).longValue());
        jdbc.update(
            "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
            ((Number) a.get("schedule_id")).longValue());
      }
    }
    jdbc.update(
        "UPDATE appointment a LEFT JOIN medical_visit v ON v.appointment_id=a.id SET"
            + " a.status=8,a.cancel_reason='系统自动标记过号' WHERE a.status=3 AND"
            + " a.appointment_date<CURRENT_DATE AND v.id IS NULL");
    reconcileBookedCounts();
    repairOrphanSlots();
    reconcilePayments();
  }

  /** 用有效预约重新计算排班已预约数，修正人工修改或异常中断造成的漂移。 */
  private void reconcileBookedCounts() {
    jdbc.update(
        "UPDATE doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt FROM appointment"
            + " WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON x.schedule_id=ds.id SET"
            + " ds.booked_count=COALESCE(x.cnt,0) WHERE ds.booked_count<>COALESCE(x.cnt,0)");
  }

  /** 时间段显示为已预约但没有有效预约关联时，恢复为空闲。 */
  private void repairOrphanSlots() {
    jdbc.update(
        "UPDATE schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND a.status IN (1,2,3,4)"
            + " SET s.status=0 WHERE s.status=1 AND a.id IS NULL");
  }

  /** 模拟支付已成功但预约已取消/过期的记录，统一改为已退款。 */
  private void reconcilePayments() {
    jdbc.update(
        "UPDATE payment_record p JOIN appointment a ON a.id=p.appointment_id SET"
            + " p.status=5,p.refunded_at=COALESCE(p.refunded_at,CURRENT_TIMESTAMP),p.refund_reason=COALESCE(p.refund_reason,'系统一致性修复')"
            + " WHERE p.status=2 AND a.status IN (6,7,8,9)");
  }
}
