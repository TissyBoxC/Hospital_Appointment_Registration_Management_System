package io.github.tissyboxc.harmsys.doctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 医生排班申请及审核结果。 */
public record ScheduleRequestResult(
    Long id,
    Long doctor_id,
    String doctor_name,
    Long department_id,
    String department_name,
    Long target_schedule_id,
    Integer request_type,
    LocalDate schedule_date,
    Integer period,
    LocalTime start_time,
    LocalTime end_time,
    Integer total_count,
    BigDecimal fee,
    String remark,
    Integer status,
    Long requested_by_user_id,
    Long reviewed_by_user_id,
    String reviewer_name,
    LocalDateTime reviewed_at,
    String review_remark,
    Long applied_schedule_id,
    LocalDateTime created_at,
    LocalDateTime updated_at) {}
