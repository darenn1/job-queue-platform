package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.config.CacheConfig;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobCacheService {

    private final JobRepository jobRepository;

    public JobCacheService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Cacheable(value = CacheConfig.JOBS_CACHE, key = "#id")
    public Job fetchJobById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @CacheEvict(value = CacheConfig.JOBS_CACHE, key = "#id")
    public void evict(UUID id) {
    }
}