package com.github.javarar.limit.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LimitSchedulerThreadExecutorTest {
  private final Logger logger = Logger.getLogger(LimitSchedulerThreadExecutor.class.getSimpleName());
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private ByteArrayOutputStream logContent;

  @BeforeEach
  public void setUp() {
    logContent = new ByteArrayOutputStream();
    StreamHandler customHandler = new StreamHandler(logContent, new Formatter() {
      @Override
      public String format(LogRecord record) {
        return String.format("%s %s \n", formatter.format(record.getInstant()), record.getMessage());
      }
    });
    LogManager.getLogManager().reset();
    logger.addHandler(customHandler);
    logger.setUseParentHandlers(false); // Отключаем обработчики по умолчанию
  }


  @Test
  public void limitScheduledTaskExecution() throws InterruptedException {
    Executor executor = LimitSchedulerThreadExecutor.limitScheduledThreadPoolExecutor(1, 1, 17);
    Runnable runnable = () -> logger.info("задача выполнилась");
    executor.execute(runnable);
    executor.execute(runnable);
    // ждем выполнения задач
    Thread.sleep(100);
    // Принудительное завершение обработчика, чтобы все логи были записаны
    for (Handler handler : logger.getHandlers()) {
      handler.flush();
    }

    String logOutput = logContent.toString();
    String regexForCompletedRunnable = "([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})(.*задача выполнилась.*)";
    Pattern pattern1 = Pattern.compile(regexForCompletedRunnable);
    Matcher matcher1 = pattern1.matcher(logOutput);
    int sum = 0;
    while (matcher1.find()){
      sum ++;
    }
    assertTrue(sum == 34, "задача была выполнена неверное количество раз");
    System.out.println(logOutput);
  }
}
