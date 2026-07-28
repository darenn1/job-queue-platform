package com.example.job_queue_platform_refined.repository.spec;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.springframework.data.jpa.domain.Specification;

public class JobSpecifications {

    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> hasType(String type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Job> filter(JobStatus status, String type) {
        return Specification.allOf(hasStatus(status), hasType(type));
    }
}