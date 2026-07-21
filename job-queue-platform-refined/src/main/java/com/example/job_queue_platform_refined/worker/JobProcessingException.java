package com.example.job_queue_platform_refined.worker;

public class JobProcessingException extends RuntimeException {

  public JobProcessingException(String message) {
    super(message);
  }

  public JobProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
  
}
