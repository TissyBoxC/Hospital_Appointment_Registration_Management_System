package io.github.tissyboxc.harmsys.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.tissyboxc.harmsys.department.entity.Department;
import io.github.tissyboxc.harmsys.department.mapper.DepartmentMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = MybatisPlusIntegrationTest.TestApplication.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:harms-mp;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.sql.init.mode=never"
    })
class MybatisPlusIntegrationTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import(MybatisPlusConfig.class)
  static class TestApplication {}

  @Autowired private DepartmentMapper mapper;
  @Autowired private DataSource dataSource;

  @BeforeEach
  void createTable() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("DROP TABLE IF EXISTS department");
    jdbc.execute(
        "CREATE TABLE department ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "parent_id BIGINT,"
            + "name VARCHAR(100) NOT NULL,"
            + "code VARCHAR(50) NOT NULL,"
            + "description VARCHAR(500),"
            + "location VARCHAR(255),"
            + "contact_phone VARCHAR(20),"
            + "sort_no INT DEFAULT 0,"
            + "status TINYINT DEFAULT 1,"
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
            + "deleted TINYINT DEFAULT 0)");
  }

  @Test
  void mapperAndPaginationWork() {
    Department first = department("内科", "INTERNAL", 1);
    Department second = department("外科", "SURGERY", 2);
    mapper.insert(first);
    mapper.insert(second);

    assertThat(first.getId()).isNotNull();
    assertThat(
            mapper.selectList(
                new LambdaQueryWrapper<Department>()
                    .eq(Department::getStatus, 1)
                    .orderByAsc(Department::getSortNo)))
        .hasSize(2);

    IPage<Department> page =
        mapper.selectPage(
            Page.of(1, 1),
            new LambdaQueryWrapper<Department>()
                .eq(Department::getStatus, 1)
                .orderByAsc(Department::getSortNo));
    assertThat(page.getTotal()).isEqualTo(2);
    assertThat(page.getRecords()).hasSize(1);
    assertThat(page.getRecords().getFirst().getName()).isEqualTo("内科");

    assertThat(mapper.deleteById(first.getId())).isEqualTo(1);
    assertThat(mapper.selectById(first.getId())).isNull();
    Integer deleted =
        new JdbcTemplate(dataSource)
            .queryForObject(
                "SELECT deleted FROM department WHERE id=?", Integer.class, first.getId());
    assertThat(deleted).isEqualTo(1);
  }

  private Department department(String name, String code, int sortNo) {
    Department department = new Department();
    department.setName(name);
    department.setCode(code);
    department.setSortNo(sortNo);
    department.setStatus(1);
    department.setDeleted(0);
    return department;
  }
}
