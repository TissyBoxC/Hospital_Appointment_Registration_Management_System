package io.github.tissyboxc.harmsys.users.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** 患者自助注册时提交的账号和身份信息。 */
public record PatientRegisterRequest(
    @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母,数字,下划线")
        String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank String confirm_password,
    @NotBlank @Size(max = 50) String real_name,
    @NotBlank @Size(max = 32) String id_card,
    @Min(0) @Max(2) Integer gender,
    @Past LocalDate birthday,
    @NotBlank @Size(max = 20) String phone,
    @Size(max = 255) String address,
    @Size(max = 50) String emergency_contact,
    @Size(max = 50) String emergency_phone) {}
