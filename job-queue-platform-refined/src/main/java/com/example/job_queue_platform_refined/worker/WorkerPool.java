package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.api.dto.WorkerStatusResponse;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
 
import java.util.List;
import java.util.UUID;

@Component
public class WorkerPool {
  private static final Logger logger = LoggerFactory.getLogger(WorkerPool.class);

  private final JobRepository jobRepository;
  private final ProcessorRegistry processorRegistry;
  private final WorkerMetrics workerMetrics;
  private final ThreadPoolTaskExecutor taskExecutor;

  public WorkerPool(JobRepository jobRepository, ProcessorRegistry processorRegistry, WorkerMetrics workerMetrics, @Qualifier("jobTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
    this.jobRepository = jobRepository;
    this.processorRegistry = processorRegistry;
    this.workerMetrics = workerMetrics;
    this.taskExecutor = taskExecutor;
  }

  @PostConstruct
  public void start() {
    logger.info("WorkerPool starting up.");
    List<Job> orphaned = jobRepository.findByStatus(JobStatus.RUNNING);
    if (!orphaned.isEmpty()) {
      orphaned.forEach(job -> {
        job.setStatus(JobStatus.PENDING);
        jobRepository.save(job);
      });
      logger.warn("Recovered {} orphaned RUNNING job(s) back to PENDING on startup.", orphaned.size());
    }
  }

  @PreDestroy
  public void shutdown() {
    logger.info("WorkerPool shutting down.");
    taskExecutor.shutdown();  
  }

  @Async("jobTaskExecutor")
  public void processJob(UUID jobId) {

    workerMetrics.jobStarted();

    Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    job.setStatus(JobStatus.RUNNING);

    jobRepository.save(job);

    try {
      JobProcessor processor = processorRegistry.getProcessor(job.getType());
      String result = processor.process(job);

      job.setStatus(JobStatus.COMPLETED);
      job.setResult(result);
      jobRepository.save(job);
      workerMetrics.jobCompleted();

    } catch (JobProcessingException ex) {
      job.setStatus(JobStatus.FAILED);
      job.setResult(ex.getMessage());
      jobRepository.save(job);
      workerMetrics.jobFailed();
      logger.warn("Job {} failed: {}", jobId, ex.getMessage());
  }
  }

  public WorkerStatusResponse getStatus() {
    long queueDepth = jobRepository.countByStatus(JobStatus.PENDING);
    return new WorkerStatusResponse(
            taskExecutor.getActiveCount(),
            workerMetrics.getJobsRunning(),
            queueDepth,
            workerMetrics.getTotalCompleted(),
            workerMetrics.getTotalFailed()
    );
}


  
}
