package com.example.job_queue_platform_refined.api.support;

import com.example.job_queue_platform_refined.domain.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobCursor(Instant lastCreatedAt, UUID lastId, JobStatus status, String type) {
}
