package io.github.tissyboxc.harmsys.patient.dto;

import jakarta.validation.constraints.NotBlank;

/** 患者可自行修改的联系方式和紧急联系人信息。 */
public record PatientProfileUpdateRequest(
    @NotBlank String real_name,
    @NotBlank String phone,
    String address,
    String emergency_contact,
    String emergency_phone) {}
