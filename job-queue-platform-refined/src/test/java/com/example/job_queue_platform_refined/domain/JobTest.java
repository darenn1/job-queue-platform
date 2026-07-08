package com.example.job_queue_platform_refined.domain;

import org.junit.jupiter.api.Test;
 
import static org.junit.jupiter.api.Assertions.*;
 
@org.junit.jupiter.api.Tag("week7")
class JobTest {
 
    @Test
    void newJobDefaultsToPendingStatus() {
        Job job = new Job("send_email", "{\"to\":\"a@b.com\"}", 1);
 
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals("send_email", job.getType());
        assertEquals(1, job.getPriority());
        assertNull(job.getId(), "id is only assigned by Hibernate on save, not on construction");
    }
 
    @Test
    void onCreateSetsCreatedAndUpdatedTimestamps() {
        Job job = new Job("resize_image", "{}", 0);
        assertNull(job.getCreatedAt());
        assertNull(job.getUpdatedAt());
 
        job.onCreate();
 
        assertNotNull(job.getCreatedAt());
        assertNotNull(job.getUpdatedAt());
        assertEquals(job.getCreatedAt(), job.getUpdatedAt(),
                "on initial creation, createdAt and updatedAt should be identical");
    }
 
    @Test
    void onUpdateAdvancesUpdatedAtButNotCreatedAt() throws InterruptedException {
        Job job = new Job("generate_report", "{}", 2);
        job.onCreate();
        var originalCreatedAt = job.getCreatedAt();
        var originalUpdatedAt = job.getUpdatedAt();
 
        Thread.sleep(5); 
        job.setStatus(JobStatus.RUNNING);
        job.onUpdate();
 
        assertEquals(originalCreatedAt, job.getCreatedAt(), "createdAt must never change after insert");
        assertTrue(job.getUpdatedAt().isAfter(originalUpdatedAt),
                "updatedAt should advance on every update");
    }
 
    @Test
    void settersMutateStatusAndResult() {
        Job job = new Job("send_email", "{}", 0);
 
        job.setStatus(JobStatus.COMPLETED);
        job.setResult("delivered");
 
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals("delivered", job.getResult());
    }
}