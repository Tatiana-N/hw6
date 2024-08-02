package com.github.javarar.rejected.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DelayedOnRejectedThreadExecutorTest {

  private final Logger logger = Logger.getLogger(DelayedOnRejectedThreadExecutor.class.getSimpleName());
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
  public void delayedAddTaskToQueue() throws InterruptedException {
    // 4 таски тк 1 тред вероятно успеет забрать задачу на исполнение
    Executor executor = DelayedOnRejectedThreadExecutor.logRejectedThreadPoolExecutor(3, 1, 10000);
    for (int i = 0; i < 4; i++) {
      int finalI = i;
      executor.execute(() -> {
        try {
          logger.info("Задача " + finalI);
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
    }
    logger.info("наполнили очередь задач");
//    эти задачи будут выполняться 8 сек, делэй стоит на 10 после них добавится еще раз лишняя задача
    Runnable runnable = () -> {
      logger.info("лишняя задача выполнилась");
    };
    executor.execute(runnable);
    // ждем выполнения задач
    Thread.sleep(15000);
    // Принудительное завершение обработчика, чтобы все логи были записаны
    for (Handler handler : logger.getHandlers()) {
      handler.flush();
    }

    String logOutput = logContent.toString();
    String regexForExtraRunnable = String.format("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})(.*задача добавлена в очередь на отложенное добавление.*%s.*)", runnable.hashCode());
    String regexForExtraRunnableToScheduledQueue = String.format("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})(.*попытка добавления задачи в очередь %s .*)", runnable.hashCode());
    Pattern pattern1 = Pattern.compile(regexForExtraRunnable);
    Pattern pattern2 = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})(.*лишняя задача выполнилась.*)");
    Pattern pattern3 = Pattern.compile(regexForExtraRunnableToScheduledQueue);
    Matcher matcher1 = pattern1.matcher(logOutput);
    Matcher matcher2 = pattern2.matcher(logOutput);
    Matcher matcher3 = pattern3.matcher(logOutput);
    assertTrue(matcher3.find(), "не нашли лог попытки добавления задачи в список выполнения");
    String timeAddTask1 = matcher3.group(1);
    assertTrue(matcher1.find(), "не нашли лог по добавлению задачи в список отложенных");
    assertTrue(matcher3.find(), "не нашли лог попытки добавления задачи в список выполнения");
    String timeAddTask2 = matcher3.group(1);
    assertTrue(matcher2.find(), "не нашли лог по выполнению отложенной задачи");
    LocalDateTime firstAddToQueue = LocalDateTime.parse(timeAddTask1, formatter);
    LocalDateTime secondAddToQueue = LocalDateTime.parse(timeAddTask2, formatter);
    Duration duration = Duration.between(firstAddToQueue.toLocalTime(), secondAddToQueue.toLocalTime());
    // проверяем что между первой и второй попыткой добавить задачу в очередь на исполнение прошло 10 сек
    assertTrue(duration.getSeconds() == 10);
  }
}
