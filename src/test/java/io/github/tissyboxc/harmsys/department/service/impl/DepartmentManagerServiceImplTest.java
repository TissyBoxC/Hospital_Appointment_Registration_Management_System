package io.github.tissyboxc.harmsys.department.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DepartmentManagerServiceImplTest {

  @Test
  void convertsJdbcDateAndTimeToJavaTimeTypes() {
    Map<String, Object> row = new HashMap<>();
    row.put("id", 1L);
    row.put("doctor_id", 2L);
    row.put("department_id", 3L);
    row.put("schedule_date", java.sql.Date.valueOf("2026-10-01"));
    row.put("period", 1);
    row.put("start_time", java.sql.Time.valueOf("09:00:00"));
    row.put("end_time", java.sql.Time.valueOf("12:00:00"));
    row.put("total_count", 20);
    row.put("booked_count", 3);
    row.put("fee", new BigDecimal("30.00"));
    row.put("status", 1);
    row.put("remark", "上午门诊");

    ScheduleResult result = DepartmentManagerServiceImpl.toScheduleResult(row);

    assertThat(result.schedule_date()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(result.start_time()).isEqualTo(LocalTime.of(9, 0));
    assertThat(result.end_time()).isEqualTo(LocalTime.of(12, 0));
  }

  @Test
  void supportsDriversThatAlreadyReturnJavaTimeTypes() {
    Map<String, Object> row = new HashMap<>();
    row.put("id", 1L);
    row.put("doctor_id", 2L);
    row.put("department_id", 3L);
    row.put("schedule_date", LocalDate.of(2026, 10, 1));
    row.put("period", 1);
    row.put("start_time", LocalTime.of(9, 0));
    row.put("end_time", LocalTime.of(12, 0));
    row.put("total_count", 20);
    row.put("booked_count", 3);
    row.put("fee", new BigDecimal("30.00"));
    row.put("status", 1);
    row.put("remark", "上午门诊");

    ScheduleResult result = DepartmentManagerServiceImpl.toScheduleResult(row);

    assertThat(result.schedule_date()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(result.start_time()).isEqualTo(LocalTime.of(9, 0));
    assertThat(result.end_time()).isEqualTo(LocalTime.of(12, 0));
  }
}
