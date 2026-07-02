package com.jobqueue.repository;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week5")
class InMemoryJobRepositoryTest {

    @Test
    void saveThenFindByIdReturnsTheJob() {
        JobRepository repository = new InMemoryJobRepository();
        Job job = new Job("send_email", "{}", 1);

        repository.save(job);
        Optional<Job> found = repository.findById(job.getId().toString());

        assertTrue(found.isPresent());
        assertEquals(job, found.get());
    }

    @Test
    void findByIdReturnsEmptyOptionalForUnknownId() {
        JobRepository repository = new InMemoryJobRepository();

        Optional<Job> found = repository.findById("does-not-exist");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdReturnsEmptyOptionalForNullId() {
        JobRepository repository = new InMemoryJobRepository();

        Optional<Job> found = repository.findById(null);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatusReturnsOnlyMatchingJobs() {
        JobRepository repository = new InMemoryJobRepository();

        Job pending1 = new Job("send_email", "{}", 1);
        Job pending2 = new Job("resize_image", "{}", 1);
        Job running = new Job("generate_report", "{}", 1);
        running.setStatus(JobStatus.RUNNING);

        repository.save(pending1);
        repository.save(pending2);
        repository.save(running);

        List<Job> pendingJobs = repository.findByStatus(JobStatus.PENDING);
        List<Job> runningJobs = repository.findByStatus(JobStatus.RUNNING);

        assertEquals(2, pendingJobs.size());
        assertTrue(pendingJobs.contains(pending1));
        assertTrue(pendingJobs.contains(pending2));

        assertEquals(1, runningJobs.size());
        assertTrue(runningJobs.contains(running));
    }

    @Test
    void findByStatusReturnsEmptyListWhenNoneMatch() {
        JobRepository repository = new InMemoryJobRepository();
        repository.save(new Job("send_email", "{}", 1));

        List<Job> failedJobs = repository.findByStatus(JobStatus.FAILED);

        assertTrue(failedJobs.isEmpty());
    }

    @Test
    void updateOverwritesTheStoredJobState() {
        JobRepository repository = new InMemoryJobRepository();
        Job job = new Job("send_email", "{}", 1);
        repository.save(job);

        job.setStatus(JobStatus.COMPLETED);
        job.setResult("delivered");
        repository.update(job);

        Optional<Job> found = repository.findById(job.getId().toString());
        assertTrue(found.isPresent());
        assertEquals(JobStatus.COMPLETED, found.get().getStatus());
        assertEquals("delivered", found.get().getResult());
    }

    @Test
    void savingNullJobThrows() {
        JobRepository repository = new InMemoryJobRepository();
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void savingJobWithNullIdThrows() {
        JobRepository repository = new InMemoryJobRepository();
        Job jobWithNoId = new Job(); // no-arg constructor leaves id null
        assertThrows(IllegalArgumentException.class, () -> repository.save(jobWithNoId));
    }

    @Test
    void concurrentSavesFromMultipleThreadsAreAllVisible() throws InterruptedException {
        JobRepository repository = new InMemoryJobRepository();
        int threadCount = 20;
        int jobsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < jobsPerThread; i++) {
                        repository.save(new Job("send_email", "{}", 1));
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should finish within timeout");
        executor.shutdown();

        assertEquals(0, errors.get(), "No thread should have thrown while saving concurrently");
        List<Job> allPending = repository.findByStatus(JobStatus.PENDING);
        assertEquals(threadCount * jobsPerThread, allPending.size(),
                "Every job from every thread should be visible with none lost");
    }
}