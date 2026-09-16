package io.github.tissyboxc.harmsys.users.dto;

/** 管理员创建账号后返回的账号与关联资料编号。 */
public record AdminCreateAccountResult(
    Long user_id, Long profile_id, String username, String role_code) {}
