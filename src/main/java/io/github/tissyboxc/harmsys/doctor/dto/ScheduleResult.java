package io.github.tissyboxc.harmsys.doctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** 排班查询返回的班次、号源和费用信息。 */
public record ScheduleResult(
    Long id,
    Long doctor_id,
    Long department_id,
    LocalDate schedule_date,
    Integer period,
    LocalTime start_time,
    LocalTime end_time,
    Integer total_count,
    Integer booked_count,
    BigDecimal fee,
    Integer status,
    String remark) {}
