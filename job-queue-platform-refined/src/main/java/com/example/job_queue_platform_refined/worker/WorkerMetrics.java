package com.example.job_queue_platform_refined.worker;

import org.springframework.stereotype.Component;
 
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkerMetrics {
  private final AtomicLong jobsRunning = new AtomicLong(0);
  private final AtomicLong totalCompleted = new AtomicLong(0);
  private final AtomicLong totalFailed = new AtomicLong(0);

  public void jobStarted() {
    jobsRunning.incrementAndGet();
  }

  public void jobCompleted() {
    jobsRunning.decrementAndGet();
    totalCompleted.incrementAndGet();
  }

  public void jobFailed() {
    jobsRunning.decrementAndGet();
    totalFailed.incrementAndGet();
  }

  public long getJobsRunning() {
    return jobsRunning.get();
  }

  public long getTotalCompleted() {
    return totalCompleted.get();
  }

  public long getTotalFailed() {
    return totalFailed.get();
  }

  
}
