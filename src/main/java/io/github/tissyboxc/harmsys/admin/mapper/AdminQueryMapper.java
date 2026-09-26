package io.github.tissyboxc.harmsys.admin.mapper;

import io.github.tissyboxc.harmsys.admin.dto.AdminUserPageQuery;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 管理员用户查询和分页数据访问。 */
public interface AdminQueryMapper {

  List<Map<String, Object>> selectUsers();

  Map<String, Object> selectUser(@Param("id") long id);

  List<String> selectRoles(@Param("id") long id);

  List<String> selectPermissions(@Param("id") long id);

  List<Map<String, Object>> selectUserPage(
      @Param("query") AdminUserPageQuery query, @Param("offset") int offset);

  long countUserPage(@Param("query") AdminUserPageQuery query);
}
