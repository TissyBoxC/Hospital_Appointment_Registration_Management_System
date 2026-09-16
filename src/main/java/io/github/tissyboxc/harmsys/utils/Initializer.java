package io.github.tissyboxc.harmsys.utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
/** 首次启动时初始化数据库结构和基础数据。 */
public class Initializer {

  private static final Logger log = LoggerFactory.getLogger(Initializer.class);
  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;
  private final DatabaseInitializationState initializationState;

  @Value("${harms.initializer.fail-fast:true}")
  private boolean failFast;

  public Initializer(
      DataSource dataSource,
      JdbcTemplate jdbcTemplate,
      DatabaseInitializationState initializationState) {
    this.dataSource = dataSource;
    this.jdbcTemplate = jdbcTemplate;
    this.initializationState = initializationState;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void initialize() throws Exception {
    initializationState.reset();
    System.out.println(
        " ______     __         ______     ______     __  __    \n"
            + "/\\  ___\\   /\\ \\       /\\  __ \\   /\\  == \\   /\\ \\/ /    \n"
            + "\\ \\ \\____  \\ \\ \\____  \\ \\  __ \\  \\ \\  __<   \\ \\  _\"-.  \n"
            + " \\ \\_____\\  \\ \\_____\\  \\ \\_\\ \\_\\  \\ \\_\\ \\_\\  \\ \\_\\ \\_\\ \n"
            + "  \\/_____/   \\/_____/   \\/_/\\/_/   \\/_/ /_/   \\/_/\\/_/ \n");

    log.info("Springboot框架加载完成,开始初始化项目");
    try {
      DatabaseInitializer();
    } catch (Exception e) {
      log.error("数据库初始化失败", e);
      if (failFast) throw new IllegalStateException("数据库初始化失败，应用未完成启动", e);
    }
  }

  private void DatabaseInitializer() throws Exception {
    log.info("检查数据库连接状态...");
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(3)) {
        throw new SQLException("数据库连接失败");
      }
      log.info("数据库连接成功");
    }
    log.info("开始执行数据库初始化脚本");
    Exception last = null;
    for (int i = 1; i <= 3; i++) {
      try {
        schemaexcute();
        last = null;
        break;
      } catch (Exception e) {
        last = e;
        log.warn("数据库初始化第{}次失败", i, e);
        try {
          Thread.sleep(1000L * i);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    if (last != null) throw last;
    applyCompatibleMigrations();
    initializationState.markInitialized();
    log.info("数据库初始化完成");
  }

  /** CREATE TABLE IF NOT EXISTS 不会给旧表补列，因此在这里执行可重复的字段迁移。 */
  private void applyCompatibleMigrations() {
    addColumnIfMissing(
        "payment_record",
        "refund_reason",
        "ALTER TABLE payment_record ADD COLUMN refund_reason VARCHAR(255) NULL COMMENT '退款原因'");
    addColumnIfMissing(
        "payment_record",
        "refund_operator_id",
        "ALTER TABLE payment_record ADD COLUMN refund_operator_id BIGINT UNSIGNED NULL COMMENT"
            + " '退款操作人'");
    widenUserTypeConstraint();
  }

  /** 旧版本 sys_user 的 CHECK 约束只允许 1~4，药房账号需要放开到 5。 */
  private void widenUserTypeConstraint() {
    try {
      jdbcTemplate.execute("ALTER TABLE sys_user DROP CHECK chk_sys_user_type");
    } catch (Exception ignored) {
      // 约束不存在或数据库版本不支持 DROP CHECK 时继续执行；新表已由 schema.sql 正确创建。
    }
    try {
      jdbcTemplate.execute(
          "ALTER TABLE sys_user ADD CONSTRAINT chk_sys_user_type CHECK (user_type IN (1,2,3,4,5))");
    } catch (Exception ignored) {
      // 已存在新约束时无需重复添加。
    }
  }

  private void addColumnIfMissing(String table, String column, String ddl) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                + " table_name=? AND column_name=?",
            Integer.class,
            table,
            column);
    if (count == null || count == 0) jdbcTemplate.execute(ddl);
  }

  private void schemaexcute() throws SQLException {
    ClassPathResource schemaScript = new ClassPathResource("config/schema.sql");
    if (!schemaScript.exists()) {
      throw new SQLException("数据库初始化脚本不存在");
    }
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setContinueOnError(false);
    populator.setIgnoreFailedDrops(false);
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.addScript(schemaScript);
    populator.execute(dataSource);
  }
}
