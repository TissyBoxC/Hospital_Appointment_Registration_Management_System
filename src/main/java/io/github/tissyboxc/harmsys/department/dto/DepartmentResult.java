package io.github.tissyboxc.harmsys.department.dto;

import java.time.LocalDateTime;

/** 科室查询返回的基本信息和层级关系。 */
public record DepartmentResult(
    Long id,
    Long parent_id,
    String name,
    String code,
    String description,
    String location,
    String contact_phone,
    Integer sort_no,
    Integer status,
    LocalDateTime created_at,
    LocalDateTime updated_at) {}
