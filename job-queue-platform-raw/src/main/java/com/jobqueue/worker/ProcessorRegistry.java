package com.jobqueue.worker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessorRegistry {
  private final Map<String, JobProcessor> processors = new ConcurrentHashMap<>();

  public void registerProcessor(String jobType, JobProcessor processor) {
      if (jobType == null || jobType.isBlank()) {
          throw new IllegalArgumentException("jobType must not be null or blank");
      }
      if (processor == null) {
          throw new IllegalArgumentException("processor must not be null");
      }
      processors.put(jobType, processor);
  }

  public JobProcessor getProcessor(String jobType) {
    JobProcessor processor = processors.get(jobType);
    if (processor == null) {
        throw new JobProcessingException("No processor registered for job type: " + jobType);
    }
    return processor;
  }

  public boolean hasProcessor(String jobType) {
    return processors.containsKey(jobType) && processors.get(jobType) != null;
  }

  public int size(){
    return processors.size();
  }
  
}
