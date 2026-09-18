package io.github.tissyboxc.harmsys.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** 保存当前 JVM 内数据库初始化是否已经完整完成。 定时任务和其他启动后任务必须在该状态为 true 后才能访问扩展表或扩展字段。
 * 单次启动可用,其实没啥用,废案来的
 */
@Component
/** 保存数据库初始化的完成状态。 */
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
