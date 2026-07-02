package com.jobqueue.repository;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import java.util.List;
import java.util.Optional;

public interface JobRepository {
  void save(Job job);

  Optional<Job> findById(String id);

  List<Job> findByStatus(JobStatus status);

  void update(Job job);

  
}
