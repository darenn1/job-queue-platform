package com.example.job_queue_platform_refined.worker;

import org.springframework.stereotype.Component;
 
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProcessorRegistry {

  private final Map<String, JobProcessor> processorMap;

  public ProcessorRegistry(List<JobProcessor> processors) {
    this.processorMap = processors.stream()
        .collect(Collectors.toMap(JobProcessor::getType, processor -> processor));
  }

  public JobProcessor getProcessor(String type) {
    JobProcessor processor = processorMap.get(type);
    if (processor == null) {
      throw new JobProcessingException("No processor found for job type: " + type);
    }
    return processor;
  }

  
}
