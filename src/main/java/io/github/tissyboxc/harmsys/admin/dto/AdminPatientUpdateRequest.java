package io.github.tissyboxc.harmsys.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** 管理员可修改的患者资料字段。 */
public record AdminPatientUpdateRequest(
    @NotBlank String real_name,
    @NotBlank String phone,
    String address,
    String emergency_contact,
    String emergency_phone) {}
