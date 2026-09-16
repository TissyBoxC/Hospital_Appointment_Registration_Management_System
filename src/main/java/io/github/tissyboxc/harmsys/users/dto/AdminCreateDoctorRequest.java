package io.github.tissyboxc.harmsys.users.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 管理员创建医生账号和医生资料的请求参数。 */
public record AdminCreateDoctorRequest(
    @NotBlank @Size(min = 4, max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotNull @Positive Long department_id,
    @NotBlank @Size(max = 50) String doctor_no,
    @NotBlank @Size(max = 50) String real_name,
    @Size(max = 50) String title,
    @Size(max = 500) String specialty,
    String introduction,
    @Size(max = 500) String avatar_url,
    @NotNull @DecimalMin("0.00") BigDecimal consultation_fee) {}
