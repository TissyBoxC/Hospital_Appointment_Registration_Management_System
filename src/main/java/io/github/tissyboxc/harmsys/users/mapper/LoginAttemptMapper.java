package io.github.tissyboxc.harmsys.users.mapper;

import java.sql.Timestamp;
import org.apache.ibatis.annotations.Param;

/** 登录失败次数和锁定状态数据访问。 */
public interface LoginAttemptMapper {

  Timestamp selectLockedUntil(
      @Param("username") String username, @Param("ipAddress") String ipAddress);

  int recordFailure(
      @Param("username") String username, @Param("ipAddress") String ipAddress);

  int deleteAttempt(
      @Param("username") String username, @Param("ipAddress") String ipAddress);
}
