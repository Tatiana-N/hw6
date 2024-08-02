package com.github.javarar.rejected.task;


import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;
public class DelayedOnRejectedThreadExecutor implements Executor {
  private final Logger logger = Logger.getLogger(DelayedOnRejectedThreadExecutor.class.getSimpleName());
  private Queue<Runnable> tasks;
  private Queue<Runnable> scheduledTasks;
  private List<Thread> threads = new LinkedList<>();
  private List<Thread> scheduledThreads = new LinkedList<>();


  private DelayedOnRejectedThreadExecutor(int sizeQueueTasks, int threadNumber, int delay) {
    tasks = new LinkedBlockingQueue<>(sizeQueueTasks);
    scheduledTasks = new LinkedBlockingQueue<>();
    for (int i = 0; i < threadNumber; i++) {
      threads.add(new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          Runnable runnable = tasks.poll();
          if(runnable != null) {
            runnable.run();
          }
        }
      }));
    }
    threads.forEach(Thread::start);
    scheduledThreads.add(new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        Runnable runnable = scheduledTasks.poll();
        if(runnable != null) {
          try {
            synchronized (runnable) {
              runnable.wait(delay);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          execute(runnable);
        }
      }
    }));
    scheduledThreads.forEach(Thread::start);
  }

  public static Executor logRejectedThreadPoolExecutor(Integer sizeQueueTasks, Integer threadNumber, Integer delay) {
    return new DelayedOnRejectedThreadExecutor(sizeQueueTasks, threadNumber, delay);
  }

  @Override
  public void execute(Runnable command) {
    try{
      logger.info("попытка добавления задачи в очередь " + command.hashCode());
      tasks.add(command);
    } catch (IllegalStateException e){
      logger.info("задача добавлена в очередь на отложенное добавление " + command.hashCode());
      scheduledTasks.add(command);
    }
  }
}
