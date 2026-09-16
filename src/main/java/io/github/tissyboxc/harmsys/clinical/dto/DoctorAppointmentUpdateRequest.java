package io.github.tissyboxc.harmsys.clinical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** 医生端修改自己预约患者及预约快照的信息。 */
public record DoctorAppointmentUpdateRequest(
    @NotBlank @Size(max = 50) String patient_name,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate appointment_date,
    @NotNull @Min(1) @Max(9) Integer status,
    @NotNull @PositiveOrZero Integer queue_no,
    @NotNull @Positive Long department_id,
    @NotNull @Min(1) @Max(3) Integer period,
    @Size(max = 500) String remark) {}
