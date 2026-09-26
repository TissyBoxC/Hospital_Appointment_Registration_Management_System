package io.github.tissyboxc.harmsys.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 新增或修改药品的请求体。 */
public record MedicineRequest(
    @NotBlank @Size(max = 50) String medicine_code,
    @NotBlank @Size(max = 255) String name,
    @Size(max = 100) String specification,
    @Size(max = 20) String unit,
    @NotNull @DecimalMin("0.00") BigDecimal unit_price,
    @NotNull @DecimalMin("0.00") BigDecimal stock_quantity,
    @NotNull @DecimalMin("0.00") BigDecimal warning_quantity,
    @Size(max = 255) String manufacturer,
    @NotNull @Min(0) @Max(1) Integer status,
    @Size(max = 500) String remark) {
  public MedicineRequest {
    if (unit == null || unit.isBlank()) unit = "盒";
  }
}
