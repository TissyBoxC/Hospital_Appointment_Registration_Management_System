package io.github.tissyboxc.harmsys.clinical.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 处方明细的药品、用量和频次信息。 */
public record PrescriptionItemRequest(
    @NotBlank @Size(max = 255) String drug_name,
    @Size(max = 100) String specification,
    @NotBlank @Size(max = 100) String dosage,
    @NotBlank @Size(max = 100) String frequency,
    @NotNull @Positive Integer days,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @Size(max = 255) String remark) {}
