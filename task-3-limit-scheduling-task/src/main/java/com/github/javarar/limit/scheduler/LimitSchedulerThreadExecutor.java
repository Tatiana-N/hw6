package com.github.javarar.limit.scheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class LimitSchedulerThreadExecutor implements Executor {
  private final Logger logger = Logger.getLogger(LimitSchedulerThreadExecutor.class.getSimpleName());
  private Queue<Queue< Runnable>> tasks;
  private List<Thread> threads = new LinkedList<>();
  private Integer limit;


  private LimitSchedulerThreadExecutor(Integer threadNumber, Integer delay, Integer limit) {
    this.limit = limit;
    tasks = new LinkedBlockingQueue<>();
    for (int i = 0; i < threadNumber; i++) {
      threads.add(new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          Queue<Runnable> poll = tasks.poll();
          if(poll != null) {
            Runnable runnable = poll.poll();
            if(runnable != null) {
              if(!poll.isEmpty()){
                tasks.add(poll);
              }
              try {
                synchronized (runnable) {
                  runnable.wait(delay);
                  runnable.run();
                }
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }));
    }
    threads.forEach(Thread::start);
  }

  public static Executor limitScheduledThreadPoolExecutor(Integer threadNumber, Integer delay, Integer limit) {
    return new LimitSchedulerThreadExecutor(threadNumber, delay, limit);
  }

  @Override
  public void execute(Runnable command) {
      logger.info("добавления задачи в очередь " + command.hashCode());
      Queue<Runnable> list = new LinkedList<>();
      for (int i = 0; i < limit; i++) {
        list.add(command);
      }
      tasks.add(list);
  }
}
