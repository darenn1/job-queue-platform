package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.api.dto.AdminJobSummaryRow;
import com.example.job_queue_platform_refined.api.support.JobCursor;
import com.example.job_queue_platform_refined.api.support.JobCursorCodec;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.repository.spec.JobSpecifications;
import com.example.job_queue_platform_refined.worker.WorkerPool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public Job submitJob(String type, String payload, int priority, UUID submittedBy) {
        Job job = new Job(type, payload, priority);
        job.setSubmittedBy(submittedBy);
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

    public Job getJob(UUID id, UUID requestingUserId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (!requestingUserId.equals(job.getSubmittedBy())) {
            throw new JobNotFoundException(id);
        }
        return job;
    }

    public List<Job> listJobs(JobStatus status) {
        return (status != null)
                ? jobRepository.findByStatus(status)
                : jobRepository.findAll();
    }

    public Page<Job> listJobsOffset(UUID submittedBy, JobStatus status, String type, Pageable pageable) {
        return jobRepository.findAll(JobSpecifications.filter(submittedBy, status, type), pageable);
    }

    public JobsKeysetPage listJobsKeyset(UUID submittedBy, JobStatus status, String type,
                                          String afterCursorEncoded, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<Job> rows;
        if (afterCursorEncoded == null) {
            rows = jobRepository.findKeysetFirstPage(submittedBy, status, type, pageable);
        } else {
            JobCursor cursor = JobCursorCodec.decode(afterCursorEncoded);
            rows = jobRepository.findKeysetAfter(submittedBy, cursor.status(), cursor.type(),
                    cursor.lastCreatedAt(), cursor.lastId(), pageable);
        }

        boolean hasMore = rows.size() > limit;
        List<Job> pageContent = hasMore ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        if (hasMore) {
            Job last = pageContent.get(pageContent.size() - 1);
            nextCursor = JobCursorCodec.encode(new JobCursor(last.getCreatedAt(), last.getId(), status, type));
        }

        return new JobsKeysetPage(pageContent, nextCursor);
    }

    public List<AdminJobSummaryRow> getAdminJobsSummary() {
        return jobRepository.findAdminJobSummaryRaw().stream()
                .map(row -> new AdminJobSummaryRow(
                        (UUID) row[0],
                        JobStatus.valueOf((String) row[1]),
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }
}