package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.domain.Job;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ResizeImageJobProcessor implements JobProcessor {

  @Override
  public String getType() {
    return "resize_image";
  }

  @Override
  public String process(Job job) throws JobProcessingException {
    simulateWork(300);
    if (ThreadLocalRandom.current().nextInt(100) < 20) {
      throw new JobProcessingException("Simulated image resize failure for job " + job.getId());
    }
    return "image resized";
  }

  private void simulateWork(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

}
