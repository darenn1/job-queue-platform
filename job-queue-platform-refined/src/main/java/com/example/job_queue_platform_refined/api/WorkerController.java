package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.WorkerStatusResponse;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkerController {
  private final WorkerPool workerPool;

  public WorkerController(WorkerPool workerPool) {
    this.workerPool = workerPool;
  }

  @GetMapping("/workers/status")
  public WorkerStatusResponse getWorkerStatus() {
    return workerPool.getStatus();
  }
  
}
