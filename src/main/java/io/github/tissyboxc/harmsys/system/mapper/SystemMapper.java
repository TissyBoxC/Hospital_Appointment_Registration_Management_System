package io.github.tissyboxc.harmsys.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 系统配置、通知和监控数据访问。 */
public interface SystemMapper {

  Integer selectDatabaseCheck();

  List<Map<String, Object>> selectNotifications(@Param("userId") long userId);

  int markNotificationRead(@Param("id") long id, @Param("userId") long userId);

  List<Map<String, Object>> selectConfigs();

  int upsertConfig(
      @Param("key") String key,
      @Param("value") String value,
      @Param("description") Object description);

  Map<String, Object> selectConfig(@Param("key") String key);

  long countUsers();

  long countPatients();

  long countDoctors();

  long countActiveSchedules();

  long countActiveAppointments();

  long countPendingOutbox();

  long countActiveSessions();
}
