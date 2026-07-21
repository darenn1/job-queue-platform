package com.example.job_queue_platform_refined.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class JobService {
  private final JobRepository jobRepository;
  private final WorkerPool workerPool;

  public JobService(JobRepository jobRepository, WorkerPool workerPool) {
      this.jobRepository = jobRepository;
      this.workerPool = workerPool;
  }

  @Transactional
  public Job submitJob(String type, String payload, int priority) {
        Job job = new Job(type, payload, priority);
        Job saved = jobRepository.save(job);

        UUID savedId = saved.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workerPool.processJob(savedId);
                }
            });
        } else {
            workerPool.processJob(savedId);
        }

        return saved;
    }
 
    public Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
 
    public List<Job> listJobs(JobStatus status) {
        return (status != null)
                ? jobRepository.findByStatus(status)
                : jobRepository.findAll();
    }
  
}
