package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Tag("week10")
class JobCacheServiceTest {

    @Autowired
    private JobCacheService jobCacheService;

    @Autowired
    private JobService jobService;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private WorkerPool workerPool; 

    @Test
    void secondFetchOfTheSameIdHitsCacheNotTheRepository() {
        UUID id = UUID.randomUUID();
        Job job = new Job("send_email", "{}", 0);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        jobCacheService.fetchJobById(id);
        jobCacheService.fetchJobById(id);
        jobCacheService.fetchJobById(id);

        verify(jobRepository, times(1)).findById(id);
    }

    @Test
    void evictForcesTheNextFetchBackToTheRepository() {
        UUID id = UUID.randomUUID();
        Job job = new Job("send_email", "{}", 0);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        jobCacheService.fetchJobById(id);
        jobCacheService.evict(id);
        jobCacheService.fetchJobById(id);

        verify(jobRepository, times(2)).findById(id);
    }

    @Test
    void differentIdsAreCachedIndependently() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        when(jobRepository.findById(idA)).thenReturn(Optional.of(new Job("send_email", "{}", 0)));
        when(jobRepository.findById(idB)).thenReturn(Optional.of(new Job("resize_image", "{}", 0)));

        jobCacheService.fetchJobById(idA);
        jobCacheService.fetchJobById(idB);
        jobCacheService.fetchJobById(idA);
        jobCacheService.fetchJobById(idB);

        verify(jobRepository, times(1)).findById(idA);
        verify(jobRepository, times(1)).findById(idB);
    }

    @Test
    void cachingNeverBypassesTheOwnershipCheck() {
        UUID jobId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        Job job = new Job("send_email", "{}", 0);
        job.setSubmittedBy(userA);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Job resultForA = jobService.getJob(jobId, userA);
        assertEquals(userA, resultForA.getSubmittedBy());

        assertThrows(JobNotFoundException.class, () -> jobService.getJob(jobId, userB));

        verify(jobRepository, times(1)).findById(jobId);
    }

    @Test
    void getJobThrowsWhenJobDoesNotExistAtAll() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJob(id, UUID.randomUUID()));
    }
}