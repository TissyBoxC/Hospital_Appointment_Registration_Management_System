package io.github.tissyboxc.harmsys.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 资料启用或禁用状态请求。 */
public record AdminStatusRequest(@NotNull @Min(0) @Max(1) Integer status) {}
