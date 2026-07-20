package com.example.job_queue_platform_refined.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class JobService {
  private final JobRepository jobRepository;

  public JobService(JobRepository jobRepository) {
      this.jobRepository = jobRepository;
  }

  @Transactional
  public Job submitJob(String type, String payload, int priority) {
        Job job = new Job(type, payload, priority);
        return jobRepository.save(job);
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
