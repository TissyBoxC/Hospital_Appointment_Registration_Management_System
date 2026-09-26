package io.github.tissyboxc.harmsys.config.database;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** 保存当前 JVM 内数据库初始化是否已经完整完成。 */
@Component
public class DatabaseInitializationState {
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  public boolean isInitialized() {
    return initialized.get();
  }

  public void markInitialized() {
    initialized.set(true);
  }

  public void reset() {
    initialized.set(false);
  }
}
