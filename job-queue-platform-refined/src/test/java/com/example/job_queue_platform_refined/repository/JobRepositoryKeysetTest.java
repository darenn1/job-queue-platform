package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Tag("week8_5")
class JobRepositoryKeysetTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void paginatingThroughAllJobsViaKeysetVisitsEveryJobExactlyOnce() {
        int totalJobs = 47;
        UUID submittedBy = UUID.randomUUID();
        for (int i = 0; i < totalJobs; i++) {
            Job job = new Job("send_email", "{}", 0);
            job.setSubmittedBy(submittedBy);
            jobRepository.save(job);
        }

        Set<UUID> seenIds = new HashSet<>();
        int pageSize = 10;

        List<Job> page = jobRepository.findKeysetFirstPage(submittedBy, null, null,
                PageRequest.of(0, pageSize));
        while (!page.isEmpty()) {
            for (Job job : page) {
                assertTrue(seenIds.add(job.getId()),
                        "job " + job.getId() + " was returned more than once across pages — duplicate");
            }

            Job last = page.get(page.size() - 1);
            page = jobRepository.findKeysetAfter(submittedBy, null, null, last.getCreatedAt(), last.getId(),
                    PageRequest.of(0, pageSize));
        }

        assertEquals(totalJobs, seenIds.size(),
                "expected every seeded job to be visited exactly once — mismatch means a gap or duplicate");
    }

    @Test
    void findKeysetFirstPage_respectsStatusFilter() {
        UUID submittedBy = UUID.randomUUID();
        Job pending = new Job("send_email", "{}", 0);
        Job failed = new Job("resize_image", "{}", 0);
        pending.setSubmittedBy(submittedBy);
        failed.setSubmittedBy(submittedBy);
        failed.setStatus(JobStatus.FAILED);
        jobRepository.save(pending);
        jobRepository.save(failed);

        List<Job> result = jobRepository.findKeysetFirstPage(submittedBy, JobStatus.FAILED, null,
                PageRequest.of(0, 10));

        assertEquals(1, result.size());
        assertEquals("resize_image", result.get(0).getType());
    }
}
