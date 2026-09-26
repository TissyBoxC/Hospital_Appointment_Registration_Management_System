package io.github.tissyboxc.harmsys.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 管理员可修改的医生资料字段。 */
public record AdminDoctorUpdateRequest(
    @NotNull Long department_id,
    @NotBlank String real_name,
    String title,
    String specialty,
    String introduction,
    String avatar_url,
    @NotNull @DecimalMin("0.00") BigDecimal consultation_fee) {}
