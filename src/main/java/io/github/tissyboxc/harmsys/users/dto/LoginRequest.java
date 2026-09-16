package io.github.tissyboxc.harmsys.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户登录时提交的账号和密码。 */
public record LoginRequest(
    @NotBlank @Size(max = 50) String username, @NotBlank(message = "密码不能为空") String password) {}
