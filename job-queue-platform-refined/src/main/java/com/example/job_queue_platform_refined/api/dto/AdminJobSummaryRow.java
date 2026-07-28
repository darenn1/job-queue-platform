package com.example.job_queue_platform_refined.api.dto;

import com.example.job_queue_platform_refined.domain.JobStatus;

import java.util.UUID;

public class AdminJobSummaryRow {

    private final UUID submittedBy;
    private final JobStatus status;
    private final long count;

    public AdminJobSummaryRow(UUID submittedBy, JobStatus status, long count) {
        this.submittedBy = submittedBy;
        this.status = status;
        this.count = count;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public JobStatus getStatus() {
        return status;
    }

    public long getCount() {
        return count;
    }
}