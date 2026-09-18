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

  //判断数据库初始化状态,未初始化时不执行定时任务
  private final DatabaseInitializationState initializationState;

  public BusinessTaskScheduler(JdbcTemplate jdbc, DatabaseInitializationState initializationState) {
    this.jdbc = jdbc;
    this.initializationState = initializationState;
  }

  @Scheduled(fixedDelayString = "${harms.jobs.interval-ms:60000}") //60秒一次
  @Transactional(rollbackFor = Exception.class)
  public void maintainBusinessStatus() {
    if (!initializationState.isInitialized()) {
      log.debug("数据库初始化尚未完成，跳过本次业务维护任务");
      return;
    }
    //开启今天即将可以开始的预约排班
    jdbc.update(
        "UPDATE doctor_schedule SET status=1 WHERE status=0 AND schedule_date=CURRENT_DATE AND"
            + " start_time>CURTIME() AND EXISTS (SELECT 1 FROM doctor d WHERE"
            + " d.id=doctor_schedule.doctor_id AND d.status=1 AND d.deleted=0)");
    //结束已经过期的排班
    jdbc.update(
        "UPDATE doctor_schedule SET status=3 WHERE status=1 AND (schedule_date<CURRENT_DATE OR"
            + " (schedule_date=CURRENT_DATE AND end_time<=CURTIME()))");
    //查询超时的预约
    List<Map<String, Object>> expired =
        jdbc.queryForList(
            "SELECT id,schedule_id,slot_id FROM appointment WHERE status IN (1,2) AND"
                + " ((appointment_date<CURRENT_DATE) OR (appointment_date=CURRENT_DATE AND"
                + " created_at < DATE_SUB(NOW(), INTERVAL 1 DAY)))");
    //逐条处理过期预约
    for (Map<String, Object> a : expired) {
      long id = ((Number) a.get("id")).longValue();
      if (jdbc.update(
              "UPDATE appointment SET status=8,cancel_reason='系统自动过期' WHERE id=? AND status IN"
                  + " (1,2)",
              id)
          == 1) {
        //减少排班已预约人数
        if (a.get("slot_id") != null)
          jdbc.update(
              "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1",
              ((Number) a.get("slot_id")).longValue());
        jdbc.update(
            "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
            ((Number) a.get("schedule_id")).longValue());
      }
    }
    //自动标记过号
    jdbc.update(
        "UPDATE appointment a LEFT JOIN medical_visit v ON v.appointment_id=a.id SET"
            + " a.status=8,a.cancel_reason='系统自动标记过号' WHERE a.status=3 AND"
            + " a.appointment_date<CURRENT_DATE AND v.id IS NULL");
    reconcileBookedCounts();
    repairOrphanSlots();
    reconcilePayments();
  }

  /**
   * 校准已预约人数,查询每个排班中的有效预约量
   */
  private void reconcileBookedCounts() {
    jdbc.update(
        "UPDATE doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt FROM appointment"
            + " WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON x.schedule_id=ds.id SET"
            + " ds.booked_count=COALESCE(x.cnt,0) WHERE ds.booked_count<>COALESCE(x.cnt,0)");
  }

  /**
   * 修复鼓励时间段,左连接后a.id为NULL:没有状态为1234的有效时间
   */
  private void repairOrphanSlots() {
    jdbc.update(
        "UPDATE schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND a.status IN (1,2,3,4)"
            + " SET s.status=0 WHERE s.status=1 AND a.id IS NULL");
  }

  /**
   * 统一处理支付成功但预约已经取消,停诊,退款,过期的退款
   */
  private void reconcilePayments() {
    jdbc.update(
        "UPDATE payment_record p JOIN appointment a ON a.id=p.appointment_id SET"
            + " p.status=5,p.refunded_at=COALESCE(p.refunded_at,CURRENT_TIMESTAMP),p.refund_reason=COALESCE(p.refund_reason,'系统一致性修复')"
            + " WHERE p.status=2 AND a.status IN (6,7,8,9)");
  }
}
