package io.github.tissyboxc.harmsys.clinical.dto;

import jakarta.validation.constraints.Size;

/** 医生更新主诉、现病史和医嘱的请求参数。 */
public record VisitUpdateRequest(
    @Size(max = 5000) String chief_complaint,
    @Size(max = 5000) String present_illness,
    @Size(max = 5000) String medical_advice) {}
