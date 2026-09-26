# MyBatis-Plus 接入说明

## 版本约束

- Spring Boot：`4.1.1`
- Java：`21`
- MyBatis-Plus：`3.5.17`
- Starter：`com.baomidou:mybatis-plus-spring-boot4-starter:3.5.17`

Spring Boot 4 必须使用 `mybatis-plus-spring-boot4-starter`，不能使用
`mybatis-plus-spring-boot3-starter`。3.5.17 的 extension 模块提供
`BaseMapper`、`IRepository` / `AbstractRepository` 和分页插件。本项目统一以
`BaseMapper` 为持久化基础，业务侧保留 `XxxService` + `XxxServiceImpl` 的普通
接口/实现结构，不额外继承 Repository 基类。

## 依赖模块

项目按官方模块拆分引入：

```groovy
implementation 'com.baomidou:mybatis-plus-spring-boot4-starter:3.5.17'
implementation 'com.baomidou:mybatis-plus-extension:3.5.17'
implementation 'com.baomidou:mybatis-plus-jsqlparser:3.5.17'
```

- `spring-boot4-starter`：Boot 4 自动配置和 Mapper 接入。
- `extension`：条件构造器、分页模型和可选的 Repository 能力。
- `jsqlparser`：MySQL 分页插件所需的 SQL 解析能力。

## 代码分层

MyBatis-Plus 模块统一使用：

```text
<module>
  entity/
  mapper/
  service/
    impl/
```

- `entity`：表实体，使用 `@TableName`、`@TableId`、`@TableLogic` 等注解。
- `mapper`：继承 `BaseMapper<T>`，只定义数据访问能力。
- `service`：业务接口，统一命名为 `XxxService`。
- `service/impl`：业务实现，统一命名为 `XxxServiceImpl`，负责事务、权限校验、
  状态流转和 Mapper 编排。

复杂 SQL 和联表查询放在：

```text
src/main/resources/mapper/<module>/XxxMapper.xml
```

不允许再新增 `XxxMybatisRepository`、`XxxRepository` 等平行持久化层。
需要同时访问多个 Mapper 时，由 `XxxServiceImpl` 组合调用，而不是再包一层 Repository。

Mapper 扫描配置在：

```text
src/main/java/io/github/tissyboxc/harmsys/config/MybatisPlusConfig.java
```

扫描范围为：

```text
io.github.tissyboxc.harmsys.**.mapper
```

## 现有代码迁移原则

项目后端已经完成统一迁移，遵循以下原则：

1. 单表基础 CRUD、逻辑删除、条件查询、分页，优先使用 MyBatis-Plus。
2. 复杂联表、报表统计、动态 SQL、批量业务流转，放在对应 `XxxMapper.xml`。
3. `FOR UPDATE`、原子扣减和状态条件更新必须保留在 XML SQL 中，不交给通用 Wrapper。
4. 事务边界统一放在 `XxxServiceImpl`，Mapper 只负责数据访问。
5. Mapper 查询必须参数化，禁止拼接用户输入。
6. 统一使用 MySQL 方言的 `LIMIT/OFFSET`、`CURRENT_TIMESTAMP` 和 `ON DUPLICATE KEY UPDATE`。

统一命名示例（以科室和医生排班为例）：

```text
department/entity/Department.java
department/mapper/DepartmentMapper.java
department/service/DepartmentService.java
department/service/impl/DepartmentServiceImpl.java

doctor/mapper/DoctorScheduleMapper.java
resources/mapper/doctor/DoctorScheduleMapper.xml
```

当前已迁移的模块包括：科室、科室负责人、患者、医生、排班、排班申请、预约、支付、
临床诊疗、药房库存与发药、挂号员工作台、用户账号、登录、权限、后台管理、系统维护、
一致性修复和公开查询接口。

业务服务的接口与实现已统一整理：

```text
admin/service/AdminAccountService.java
admin/service/impl/AdminAccountServiceImpl.java

appointment/service/AppointmentService.java
appointment/service/impl/AppointmentServiceImpl.java

clinical/service/ClinicalService.java
clinical/service/impl/ClinicalServiceImpl.java
```

Controller 只注入 `XxxService`，不直接依赖 Mapper，也不直接拼写 SQL。
单表 CRUD 使用 `BaseMapper` 和条件构造器；联表、行锁、分页、库存扣减和状态流转
保留在对应 `XxxMapper.xml` 或 Mapper 注解 SQL 中。

## 分页

分页插件由 `MybatisPlusConfig` 注册：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
  MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
  interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
  return interceptor;
}
```

分页查询示例：

```java
IPage<Department> page = mapper.selectPage(Page.of(1, 20), wrapper);
```

## 逻辑删除

表实体中的删除标记使用：

```java
@TableLogic
private Integer deleted;
```

执行 `deleteById` 时会自动转换为逻辑删除更新。对于 Mapper XML 中的手写 SQL，
需要继续显式写 `deleted = 0` 条件，因为逻辑删除插件不会改写所有自定义 SQL。

## 构建

项目统一使用本地 Gradle：

```text
D:\service\gradle-9.4.1\bin\gradle.bat
```

日常构建优先离线执行：

```text
D:\service\gradle-9.4.1\bin\gradle.bat clean test --offline --console plain
```

当前依赖已解析完成，后续默认离线构建。新增依赖或清理依赖缓存后，
才需要先联网完成依赖解析，再恢复离线构建。

## 验证

`MybatisPlusIntegrationTest` 使用 H2 内存数据库验证：

- Mapper 扫描
- BaseMapper 插入
- Lambda 条件查询
- 分页插件
- 逻辑删除

完整测试命令：

```text
D:\service\gradle-9.4.1\bin\gradle.bat clean test --offline --console plain
```

XML 变更后至少执行一次完整 `clean test`，以验证 Mapper 扫描、XML 加载、
方法签名绑定和分页插件。

迁移审计可以执行：

```text
rg -n "JdbcTemplate|NamedParameterJdbcTemplate|@Repository|queryForList|queryForObject" src/main/java
rg -n --glob "*Service.java" "^public class|^public interface|@Service" src/main/java
```

预期结果：

- 生产代码中没有 `JdbcTemplate`、`NamedParameterJdbcTemplate`、`@Repository`。
- `service` 目录下只有 `XxxService` 接口。
- 具体实现只存在于 `service/impl` 下的 `XxxServiceImpl`。
