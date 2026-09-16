package io.github.tissyboxc.harmsys.clinical.dto;

import jakarta.validation.constraints.*;

/** 为指定就诊记录创建处方。 */
public record PrescriptionRequest(@NotNull @Positive Long visit_id) {}
