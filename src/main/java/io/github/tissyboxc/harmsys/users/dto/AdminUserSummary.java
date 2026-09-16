package io.github.tissyboxc.harmsys.users.dto;

import java.util.List;

/** 管理端用户列表中的账号、资料和权限摘要。 */
public record AdminUserSummary(
    Long user_id,
    String username,
    Integer user_type,
    Integer status,
    String display_name,
    Long patient_id,
    Long doctor_id,
    List<String> role_codes,
    List<String> permission_codes) {}
