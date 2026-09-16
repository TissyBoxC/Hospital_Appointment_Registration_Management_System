package io.github.tissyboxc.harmsys.doctor.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;

/** 在排班下创建时间段的请求参数。 */
public record SlotRequest(
    @NotNull @Positive Integer slot_no,
    @NotNull LocalTime start_time,
    @NotNull LocalTime end_time) {}
