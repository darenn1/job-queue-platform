package com.example.job_queue_platform_refined.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(UUID id) {
        super("Job not found: " + id);
    }

  
}
