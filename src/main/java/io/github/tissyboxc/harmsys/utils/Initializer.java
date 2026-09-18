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
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
/** 首次启动时初始化数据库结构和基础数据。 */
public class Initializer {

  private static final Logger log = LoggerFactory.getLogger(Initializer.class);
  private final DataSource dataSource;
  private final DatabaseInitializationState initializationState;

  @Value("${harms.initializer.fail-fast:true}")
  private boolean failFast;

  public Initializer(
      DataSource dataSource,
      DatabaseInitializationState initializationState) {
    this.dataSource = dataSource;
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
    initializationState.markInitialized();
    log.info("数据库初始化完成");
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
