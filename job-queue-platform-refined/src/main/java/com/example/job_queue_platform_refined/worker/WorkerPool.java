package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.api.dto.WorkerStatusResponse;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.service.JobCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);
    private static final Duration DEQUEUE_TIMEOUT = Duration.ofSeconds(5);

    private final JobRepository jobRepository;
    private final ProcessorRegistry processorRegistry;
    private final WorkerMetrics workerMetrics;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final JobCacheService jobCacheService;
    private final RedisJobQueue redisJobQueue;
    private final int consumerThreadCount;
    private final int maxRetries;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public WorkerPool(JobRepository jobRepository,
                       ProcessorRegistry processorRegistry,
                       WorkerMetrics workerMetrics,
                       @Qualifier("jobTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
                       JobCacheService jobCacheService,
                       RedisJobQueue redisJobQueue,
                       @Value("${worker.consumer-threads:4}") int consumerThreadCount,
                       @Value("${worker.max-retries:3}") int maxRetries) {
        this.jobRepository = jobRepository;
        this.processorRegistry = processorRegistry;
        this.workerMetrics = workerMetrics;
        this.taskExecutor = taskExecutor;
        this.jobCacheService = jobCacheService;
        this.redisJobQueue = redisJobQueue;
        this.consumerThreadCount = consumerThreadCount;
        this.maxRetries = maxRetries;
    }

    @PostConstruct
    public void start() {
        log.info("WorkerPool starting {} consumer thread(s).", consumerThreadCount);

        List<Job> orphaned = jobRepository.findByStatus(JobStatus.RUNNING);
        if (!orphaned.isEmpty()) {
            orphaned.forEach(job -> {
                job.setStatus(JobStatus.PENDING);
                jobRepository.save(job);
                jobCacheService.evict(job.getId());
                redisJobQueue.enqueue(job.getId());
            });
            log.warn("Recovered {} orphaned RUNNING job(s) — reset to PENDING and re-enqueued.", orphaned.size());
        }

        running.set(true);
        for (int i = 0; i < consumerThreadCount; i++) {
            taskExecutor.execute(this::consumerLoop);
        }
    }

    private void consumerLoop() {
        while (running.get()) {
            try {
                Optional<UUID> jobId = redisJobQueue.dequeue(DEQUEUE_TIMEOUT);
                jobId.ifPresent(this::processJob);
            } catch (Exception ex) {
                log.error("Unexpected error in worker consumer loop", ex);
            }
        }
        log.info("Worker consumer loop exiting.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("WorkerPool shutting down — signaling consumer loops to stop.");
        running.set(false);
        taskExecutor.shutdown();
    }

    public void processJob(UUID jobId) {
        workerMetrics.jobStarted();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
        jobCacheService.evict(jobId);

        try {
            JobProcessor processor = processorRegistry.getProcessor(job.getType());
            String result = processor.process(job);

            job.setStatus(JobStatus.COMPLETED);
            job.setResult(result);
            jobRepository.save(job);
            jobCacheService.evict(jobId);
            workerMetrics.jobCompleted();

        } catch (JobProcessingException ex) {
            handleFailure(job, ex);
        }
    }

    private void handleFailure(Job job, JobProcessingException ex) {
        int attemptsSoFar = job.getRetryCount() + 1;

        if (attemptsSoFar <= maxRetries) {
            job.setRetryCount(attemptsSoFar);
            job.setStatus(JobStatus.PENDING);
            job.setResult(ex.getMessage());
            jobRepository.save(job);
            jobCacheService.evict(job.getId());
            workerMetrics.jobFailed();

            redisJobQueue.enqueue(job.getId());

            log.warn("Job {} failed (attempt {}/{}), re-enqueued for retry: {}",
                    job.getId(), attemptsSoFar, maxRetries, ex.getMessage());

        } else {
            job.setStatus(JobStatus.FAILED);
            job.setResult(ex.getMessage());
            jobRepository.save(job);
            jobCacheService.evict(job.getId());
            workerMetrics.jobFailed();

            redisJobQueue.enqueueDeadLetter(job.getId());

            log.warn("Job {} failed permanently after {} attempts, moved to dead-letter: {}",
                    job.getId(), attemptsSoFar, ex.getMessage());
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