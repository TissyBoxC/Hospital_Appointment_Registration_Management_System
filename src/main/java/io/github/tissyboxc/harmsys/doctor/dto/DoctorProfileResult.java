package io.github.tissyboxc.harmsys.doctor.dto;

import java.math.BigDecimal;

/** 医生端返回的完整资料信息。 */
public record DoctorProfileResult(
    Long id,
    Long user_id,
    Long department_id,
    String doctor_no,
    String real_name,
    String title,
    String specialty,
    String introduction,
    String avatar_url,
    BigDecimal consultation_fee,
    Integer status) {}
