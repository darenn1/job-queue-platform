package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Tag("week7")
class JobRepositoryTest {
 
    @Autowired
    private JobRepository jobRepository;
 
    @Test
    void saveAssignsGeneratedIdAndTimestamps() {
        Job job = new Job("send_email", "{\"to\":\"a@b.com\"}", 1);
 
        Job saved = jobRepository.save(job);
 
        assertNotNull(saved.getId(), "Hibernate should assign a UUID on insert");
        assertNotNull(saved.getCreatedAt(), "@PrePersist should have fired");
        assertNotNull(saved.getUpdatedAt());
        assertEquals(JobStatus.PENDING, saved.getStatus());
    }
 
    @Test
    void findByIdReturnsPersistedJob() {
        Job saved = jobRepository.save(new Job("resize_image", "{}", 2));
 
        var found = jobRepository.findById(saved.getId());
 
        assertTrue(found.isPresent());
        assertEquals("resize_image", found.get().getType());
    }
 
    @Test
    void findByStatusReturnsOnlyMatchingJobs() {
        Job pending = new Job("send_email", "{}", 0);
        Job failed = new Job("generate_report", "{}", 0);
        failed.setStatus(JobStatus.FAILED);
 
        jobRepository.save(pending);
        jobRepository.save(failed);
 
        List<Job> failedJobs = jobRepository.findByStatus(JobStatus.FAILED);
 
        assertEquals(1, failedJobs.size());
        assertEquals("generate_report", failedJobs.get(0).getType());
    }
 
    @Test
    void updatingAJobAdvancesUpdatedAtOnFlush() {
        Job saved = jobRepository.save(new Job("send_email", "{}", 0));
        var originalUpdatedAt = saved.getUpdatedAt();
 
        saved.setStatus(JobStatus.RUNNING);
        Job updated = jobRepository.saveAndFlush(saved);
 
        assertEquals(JobStatus.RUNNING, updated.getStatus());
        assertTrue(!updated.getUpdatedAt().isBefore(originalUpdatedAt),
                "@PreUpdate should keep updatedAt current");
    }
}
 
