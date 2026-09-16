package io.github.tissyboxc.harmsys.users.dto;

/** 登录校验所需的账号、密码摘要和资料编号。 */
public record LoginUserRecord(
    Long user_id,
    String username,
    String password_hash,
    Integer user_type,
    Integer status,
    Long patient_id,
    Long doctor_id,
    Integer doctor_status,
    String display_name) {}
