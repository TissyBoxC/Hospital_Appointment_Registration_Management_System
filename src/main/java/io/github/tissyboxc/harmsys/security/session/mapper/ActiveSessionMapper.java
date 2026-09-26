package io.github.tissyboxc.harmsys.security.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.security.session.entity.ActiveSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** active_session 表数据访问。 */
public interface ActiveSessionMapper extends BaseMapper<ActiveSession> {

  @Select(
      "SELECT COUNT(*) FROM active_session s JOIN sys_user u ON u.id=s.user_id"
          + " WHERE s.session_id=#{sessionId} AND s.user_id=#{userId}"
          + " AND u.status=1 AND u.deleted=0 AND s.expires_at>CURRENT_TIMESTAMP")
  long countValid(@Param("sessionId") String sessionId, @Param("userId") long userId);
}
