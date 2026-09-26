package io.github.tissyboxc.harmsys.admin.dto;

/** 管理员用户分页查询条件。 */
public record AdminUserPageQuery(
    int page, int pageSize, Integer userType, Integer status, String keyword) {}
