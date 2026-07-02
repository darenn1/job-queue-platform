package com.jobqueue.repository;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryJobRepository implements JobRepository {
  private final Map<String, Job> jobStore = new ConcurrentHashMap<>();

  public void save(Job job) {
    if (job == null || job.getId() == null) {
      throw new IllegalArgumentException("Job or Job ID cannot be null");
    }
    jobStore.put(job.getId().toString(), job);
  }

  public Optional<Job> findById(String id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(jobStore.get(id));
  }

  public List<Job> findByStatus(JobStatus status) {
    return jobStore.values().stream()
            .filter(job -> job.getStatus() == status)
            .collect(Collectors.toList());
  }

  public void update(Job job) {
    save(job);
  }


  
}
