package io.github.tissyboxc.harmsys.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** 库存入库请求体。 */
public record StockInRequest(@NotNull @Positive BigDecimal quantity) {}
