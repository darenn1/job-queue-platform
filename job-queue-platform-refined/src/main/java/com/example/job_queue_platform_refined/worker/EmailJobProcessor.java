package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.domain.Job;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class EmailJobProcessor implements JobProcessor {

  @Override
  public String getType() {
    return "send_email";
  }

  @Override
  public String process(Job job) throws JobProcessingException {
    simulateWork(200);
    if (ThreadLocalRandom.current().nextInt(100) < 20) {
            throw new JobProcessingException("Simulated email delivery failure for job " + job.getId());
        }
        return "email sent";

  }

  private void simulateWork(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }


  
}
