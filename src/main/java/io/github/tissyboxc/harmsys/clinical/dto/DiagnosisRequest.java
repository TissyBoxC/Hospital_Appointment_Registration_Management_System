package io.github.tissyboxc.harmsys.clinical.dto;

import jakarta.validation.constraints.*;

/** 医生保存诊断信息时提交的请求参数。 */
public record DiagnosisRequest(
    @NotBlank @Size(max = 255) String diagnosis_name,
    @Size(max = 50) String diagnosis_code,
    @NotNull @Min(1) @Max(2) Integer diagnosis_type,
    @Size(max = 500) String remark) {}
