package io.github.tissyboxc.harmsys.department.dto;

import jakarta.validation.constraints.*;

/** 创建或更新科室时提交的请求参数。 */
public record DepartmentRequest(
    Long parent_id,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String code,
    @Size(max = 500) String description,
    @Size(max = 255) String location,
    @Size(max = 20) String contact_phone,
    @NotNull @Min(0) Integer sort_no,
    @NotNull @Min(0) @Max(1) Integer status) {}
