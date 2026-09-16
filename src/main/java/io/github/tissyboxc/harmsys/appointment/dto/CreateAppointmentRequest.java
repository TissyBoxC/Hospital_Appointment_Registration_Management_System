package io.github.tissyboxc.harmsys.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 患者创建预约时提交的排班、时间段和请求信息。 */
public record CreateAppointmentRequest(
    @NotNull @Positive Long schedule_id,
    Long slot_id,
    @Size(max = 500) String remark,
    @Size(max = 80) String request_no) {}
