package com.example.job_queue_platform_refined.worker;

import com.example.job_queue_platform_refined.api.dto.WorkerStatusResponse;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @Tag("week7") — Day 36/37 tests. Note @Async has NO effect here: it only
 * takes effect through a Spring AOP proxy, and this test constructs
 * WorkerPool directly with `new WorkerPool(...)`, so processJob() below
 * runs synchronously on the test thread. That's exactly what we want for a
 * deterministic unit test — no waiting, no timing flakiness, no need for
 * Awaitility or Thread.sleep in the test itself.
 */
@ExtendWith(MockitoExtension.class)
@Tag("week7")
class WorkerPoolTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private ProcessorRegistry processorRegistry;
    @Mock
    private WorkerMetrics workerMetrics;
    @Mock
    private ThreadPoolTaskExecutor taskExecutor;

    @Test
    void processJob_onSuccess_marksCompletedAndRecordsMetrics() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);
        UUID id = UUID.randomUUID();
        Job job = new Job("send_email", "{}", 0);
        JobProcessor processor = mock(JobProcessor.class);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(processorRegistry.getProcessor("send_email")).thenReturn(processor);
        when(processor.process(job)).thenReturn("email sent");

        pool.processJob(id);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals("email sent", job.getResult());
        verify(workerMetrics).jobStarted();
        verify(workerMetrics).jobCompleted();
        verify(workerMetrics, never()).jobFailed();
        // saved twice: once transitioning to RUNNING, once to COMPLETED
        verify(jobRepository, times(2)).save(job);
    }

    @Test
    void processJob_onProcessorFailure_marksFailedAndRecordsMetrics() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);
        UUID id = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        JobProcessor processor = mock(JobProcessor.class);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(processorRegistry.getProcessor("resize_image")).thenReturn(processor);
        when(processor.process(job)).thenThrow(new JobProcessingException("simulated failure"));

        pool.processJob(id);

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals("simulated failure", job.getResult());
        verify(workerMetrics).jobStarted();
        verify(workerMetrics).jobFailed();
        verify(workerMetrics, never()).jobCompleted();
    }

    @Test
    void processJob_throwsJobNotFoundException_whenJobDoesNotExist() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> pool.processJob(id));
        // jobStarted() already fired before the lookup — this documents
        // current behavior (metrics can drift if a job vanishes mid-flight)
        // rather than asserting it's ideal; worth revisiting once retry/DLQ
        // logic exists in Week 6-equivalent hardening.
        verify(workerMetrics).jobStarted();
    }

    @Test
    void start_recoversOrphanedRunningJobsBackToPending() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);
        Job orphaned = new Job("send_email", "{}", 0);
        orphaned.setStatus(JobStatus.RUNNING);
        when(jobRepository.findByStatus(JobStatus.RUNNING)).thenReturn(List.of(orphaned));

        pool.start();

        assertEquals(JobStatus.PENDING, orphaned.getStatus());
        verify(jobRepository).save(orphaned);
    }

    @Test
    void start_doesNothingWhenNoOrphanedJobsExist() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);
        when(jobRepository.findByStatus(JobStatus.RUNNING)).thenReturn(List.of());

        pool.start();

        verify(jobRepository, never()).save(any());
    }

    @Test
    void shutdown_delegatesToTaskExecutor() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);

        pool.shutdown();

        verify(taskExecutor).shutdown();
    }

    @Test
    void getStatus_assemblesAllFiveFieldsFromItsCollaborators() {
        WorkerPool pool = new WorkerPool(jobRepository, processorRegistry, workerMetrics, taskExecutor);

        when(taskExecutor.getActiveCount()).thenReturn(2);
        when(workerMetrics.getJobsRunning()).thenReturn(3L);
        when(jobRepository.countByStatus(JobStatus.PENDING)).thenReturn(5L);
        when(workerMetrics.getTotalCompleted()).thenReturn(10L);
        when(workerMetrics.getTotalFailed()).thenReturn(1L);

        WorkerStatusResponse status = pool.getStatus();

        assertEquals(2, status.getActiveWorkers());
        assertEquals(3L, status.getJobsRunning());
        assertEquals(5L, status.getQueueDepth());
        assertEquals(10L, status.getTotalCompleted());
        assertEquals(1L, status.getTotalFailed());
    }
}