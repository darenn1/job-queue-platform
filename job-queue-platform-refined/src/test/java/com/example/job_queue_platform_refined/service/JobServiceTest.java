package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week9")
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private WorkerPool workerPool;
    @Mock
    private JobCacheService jobCacheService;

    @Test
    void submitJobStampsTheSubmittingUsersIdOnTheJob() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID submittedBy = UUID.randomUUID();
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = service.submitJob("send_email", "{}", 2, submittedBy);

        assertEquals(submittedBy, result.getSubmittedBy());
        verify(workerPool, times(1)).processJob(any());
    }

    @Test
    void getJobReturnsJobWhenCallerIsTheSubmitter() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        job.setSubmittedBy(owner);
        when(jobCacheService.fetchJobById(id)).thenReturn(job);

        Job result = service.getJob(id, owner);

        assertEquals("resize_image", result.getType());
    }

    @Test
    void getJobThrowsNotFound_whenCallerIsNotTheSubmitter_evenThoughTheJobExists() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID someoneElse = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        job.setSubmittedBy(owner);
        when(jobCacheService.fetchJobById(id)).thenReturn(job);

        assertThrows(JobNotFoundException.class, () -> service.getJob(id, someoneElse));
    }

    @Test
    void getJobThrowsNotFoundWhenJobDoesNotExistAtAll() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID id = UUID.randomUUID();
        when(jobCacheService.fetchJobById(id)).thenThrow(new JobNotFoundException(id));

        assertThrows(JobNotFoundException.class, () -> service.getJob(id, UUID.randomUUID()));
    }

    @Test
    void listJobsOffsetDelegatesToRepositoryWithSpecificationAndPageable() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID submittedBy = UUID.randomUUID();
        when(jobRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.listJobsOffset(submittedBy, JobStatus.FAILED, "send_email", PageRequest.of(0, 20));

        verify(jobRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void listJobsKeysetPassesSubmittedByThroughOnFirstPage() {
        JobService service = new JobService(jobRepository, workerPool, jobCacheService);
        UUID submittedBy = UUID.randomUUID();
        when(jobRepository.findKeysetFirstPage(eq(submittedBy), any(), any(), any())).thenReturn(List.of());

        service.listJobsKeyset(submittedBy, null, null, null, 20);

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(jobRepository).findKeysetFirstPage(captor.capture(), any(), any(), any());
        assertEquals(submittedBy, captor.getValue());
    }
}
