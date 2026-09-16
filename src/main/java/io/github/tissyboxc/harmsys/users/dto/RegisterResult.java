package io.github.tissyboxc.harmsys.users.dto;

/** 患者注册成功后返回的账号和患者编号。 */
public record RegisterResult(Long user_id, Long patient_id, String username) {}
