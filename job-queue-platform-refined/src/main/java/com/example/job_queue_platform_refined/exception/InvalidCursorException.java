package com.example.job_queue_platform_refined.exception;

public class InvalidCursorException extends RuntimeException {
  public InvalidCursorException(String cursor, Throwable cause) {
        super("Invalid pagination cursor: " + cursor, cause);
    }
  
}
