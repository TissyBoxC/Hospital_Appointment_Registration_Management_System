package io.github.tissyboxc.harmsys.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员创建药房账号的请求参数。 */
public record AdminCreatePharmacyRequest(
    @NotBlank @Size(min = 4, max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
    @NotBlank @Size(min = 8, max = 64) String password) {}
