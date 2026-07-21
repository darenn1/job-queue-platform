package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);


  
} 
