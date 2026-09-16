package io.github.tissyboxc.harmsys.doctor.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 医生更新个人资料时提交的请求参数。 */
public record DoctorProfileUpdateRequest(
    @NotBlank @Size(max = 50) String real_name,
    @Size(max = 50) String title,
    @Size(max = 500) String specialty,
    String introduction,
    @Size(max = 500) String avatar_url,
    @NotNull @DecimalMin("0.00") BigDecimal consultation_fee) {}
