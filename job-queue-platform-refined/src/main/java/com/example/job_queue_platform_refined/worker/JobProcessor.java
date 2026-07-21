package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.domain.Job;

public interface JobProcessor {
  String getType();

  String process(Job job) throws JobProcessingException;
  
}
