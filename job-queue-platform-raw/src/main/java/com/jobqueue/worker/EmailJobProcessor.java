package com.jobqueue.worker;

import com.jobqueue.domain.Job;
import java.util.concurrent.ThreadLocalRandom;

public class EmailJobProcessor implements JobProcessor {

  private final long sleepMillis;
  private final double failureProbability;
 
  public EmailJobProcessor() {
      this(200, 0.3);
  }
 
  public EmailJobProcessor(long sleepMillis, double failureProbability) {
      this.sleepMillis = sleepMillis;
      this.failureProbability = failureProbability;
  }
 
  @Override
  public String process(Job job) throws JobProcessingException {
      simulateNetworkDelay();
      if (ThreadLocalRandom.current().nextDouble() < failureProbability) {
          throw new JobProcessingException("Simulated email delivery failure for job " + job.getId());
      }
      return "Email sent for job " + job.getId();
  }
 
  private void simulateNetworkDelay() {
    try {
        Thread.sleep(sleepMillis);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new JobProcessingException("Interrupted while sending email for job", e);
    }
  }


  
}
