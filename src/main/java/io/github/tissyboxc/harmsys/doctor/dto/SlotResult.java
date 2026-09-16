package io.github.tissyboxc.harmsys.doctor.dto;

import java.time.LocalTime;

/** 时间段查询返回的时间和状态信息。 */
public record SlotResult(
    Long id,
    Long schedule_id,
    Integer slot_no,
    LocalTime start_time,
    LocalTime end_time,
    Integer status) {}
