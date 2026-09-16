package io.github.tissyboxc.harmsys.users.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 登录成功后返回的身份、角色和跳转信息。 */
public record LoginResult(
    Long user_id,
    String username,
    String display_name,
    Integer user_type,
    String primary_role,
    List<String> role_codes,
    Long patient_id,
    Long doctor_id,
    String redirect_path,
    Integer session_timeout_seconds,
    LocalDateTime long_time) {}
