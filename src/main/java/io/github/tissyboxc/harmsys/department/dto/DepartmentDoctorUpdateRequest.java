package io.github.tissyboxc.harmsys.department.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 科室负责人可修改的医生资料字段。 */
public record DepartmentDoctorUpdateRequest(
    @NotBlank String real_name,
    String title,
    String specialty,
    String introduction,
    String avatar_url,
    @NotNull @DecimalMin("0.00") BigDecimal consultation_fee,
    @NotNull Integer status) {}
