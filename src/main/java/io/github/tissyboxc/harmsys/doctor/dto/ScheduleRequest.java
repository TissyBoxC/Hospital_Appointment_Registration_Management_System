package io.github.tissyboxc.harmsys.doctor.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** 创建或更新医生排班的请求参数。 */
public record ScheduleRequest(
    Long doctor_id,
    @NotNull @Positive Long department_id,
    @NotNull LocalDate schedule_date,
    @NotNull @Min(1) @Max(3) Integer period,
    @NotNull LocalTime start_time,
    @NotNull LocalTime end_time,
    @NotNull @Positive Integer total_count,
    @NotNull @DecimalMin("0.00") BigDecimal fee,
    @Size(max = 255) String remark) {}
