package io.github.tissyboxc.harmsys.users.sessions;

import java.io.Serializable;
import java.util.List;

/** 保存在会话中的当前用户身份和角色信息。 */
public record AuthenticatedUser(
    Long user_id,
    String username,
    String display_name,
    Integer user_type,
    String primary_role,
    List<String> role_codes,
    Long patient_id,
    Long doctor_id)
    implements Serializable {
  public AuthenticatedUser {
    role_codes = List.copyOf(role_codes);
  }
}
