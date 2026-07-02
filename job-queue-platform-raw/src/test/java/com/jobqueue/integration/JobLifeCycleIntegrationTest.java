package com.jobqueue.integration;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import com.jobqueue.queue.InMemoryJobQueue;
import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.InMemoryJobRepository;
import com.jobqueue.repository.JobRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week5")
class JobLifecycleIntegrationTest {

    @Test
    void fiveJobsFlowFromPendingThroughRunningToCompleted() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();

        List<Job> submittedJobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Job job = new Job("send_email", "{\"to\":\"user" + i + "@example.com\"}", 1);
            repository.save(job);
            queue.enqueue(job);
            submittedJobs.add(job);
        }

        List<Job> pendingJobs = repository.findByStatus(JobStatus.PENDING);
        assertEquals(5, pendingJobs.size(), "All 5 submitted jobs should be PENDING before processing");

        for (int i = 0; i < 5; i++) {
            Job dequeued = queue.dequeue();
            assertNotNull(dequeued);


            dequeued.setStatus(JobStatus.RUNNING);
            repository.update(dequeued);

            Optional<Job> runningSnapshot = repository.findById(dequeued.getId().toString());
            assertTrue(runningSnapshot.isPresent());
            assertEquals(JobStatus.RUNNING, runningSnapshot.get().getStatus());

            Thread.sleep(20);

            dequeued.setStatus(JobStatus.COMPLETED);
            dequeued.setResult("delivered to " + dequeued.getPayload());
            repository.update(dequeued);
        }

        assertTrue(queue.isEmpty(), "Queue should be empty after dequeuing all submitted jobs");

        assertEquals(0, repository.findByStatus(JobStatus.PENDING).size());
        assertEquals(0, repository.findByStatus(JobStatus.RUNNING).size());

        List<Job> completedJobs = repository.findByStatus(JobStatus.COMPLETED);
        assertEquals(5, completedJobs.size(), "All 5 jobs should have transitioned to COMPLETED");


        for (Job original : submittedJobs) {
            Optional<Job> finalState = repository.findById(original.getId().toString());
            assertTrue(finalState.isPresent());
            Job job = finalState.get();

            assertEquals(JobStatus.COMPLETED, job.getStatus());
            assertNotNull(job.getResult());
            assertTrue(job.getUpdatedAt().isAfter(job.getCreatedAt()) || job.getUpdatedAt().equals(job.getCreatedAt()),
                    "updatedAt should never be before createdAt");
        }
    }
}