package io.github.tissyboxc.harmsys.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.department.entity.Department;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** department 表数据访问。 */
public interface DepartmentMapper extends BaseMapper<Department> {

  @Select("SELECT COUNT(*) FROM doctor WHERE department_id = #{departmentId} AND deleted = 0")
  Long countDoctors(@Param("departmentId") long departmentId);
}
