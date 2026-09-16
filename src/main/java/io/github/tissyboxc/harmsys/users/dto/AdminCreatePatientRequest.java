package io.github.tissyboxc.harmsys.users.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** 管理员创建患者账号和患者资料的请求参数。 */
public record AdminCreatePatientRequest(
    @NotBlank @Size(min = 4, max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank @Size(max = 50) String real_name,
    @NotBlank @Size(max = 32) String id_card,
    @Min(0) @Max(2) Integer gender,
    @Past LocalDate birthday,
    @NotBlank @Size(max = 20) String phone,
    @Size(max = 255) String address,
    @Size(max = 50) String emergency_contact,
    @Size(max = 20) String emergency_phone) {}
