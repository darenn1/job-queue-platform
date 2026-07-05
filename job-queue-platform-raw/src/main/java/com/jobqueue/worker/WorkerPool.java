package com.jobqueue.worker;

import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.JobRepository;
 
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkerPool {
  private final int poolSize;
  private final JobQueue queue;
  private final JobRepository repository;
  private final ProcessorRegistry registry;
  private final List<WorkerThread> workers = new ArrayList<>();

  public WorkerPool(int poolSize, JobQueue queue, JobRepository repository, ProcessorRegistry registry) {
    if (poolSize <= 0) {
        throw new IllegalArgumentException("poolSize must be positive");
    }
    this.poolSize = poolSize;
    this.queue = queue;
    this.repository = repository;
    this.registry = registry;
  }

  public synchronized void start() {
    if (!workers.isEmpty()) {
        throw new IllegalStateException("Worker pool is already running");
    }
    for (int i = 0; i < poolSize; i++) {
        WorkerThread worker = new WorkerThread("Worker-" + i, queue, repository, registry);
        workers.add(worker);
        worker.start();
    }
  }

  public synchronized boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
    for (WorkerThread worker : workers) {
      worker.requestStop();
    }
    for (WorkerThread worker : workers) {
      worker.interrupt();
    }

    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
    boolean allStopped = true;
    for (WorkerThread worker : workers) {
      long remainingNanos = deadlineNanos - System.nanoTime();
      if (remainingNanos <= 0) {
          if (worker.isAlive()) {
              allStopped = false;
          }
          continue;
      }
      long remainingMillis = Math.max(1L, remainingNanos / 1_000_000L);
      worker.join(remainingMillis);
      if (worker.isAlive()) {
          allStopped = false;
      }
    }
    return allStopped;
  }

  public int size() {
    return workers.size();
  }

  public List<WorkerThread> getWorkers() {
    return List.copyOf(workers);
  }
  
}
