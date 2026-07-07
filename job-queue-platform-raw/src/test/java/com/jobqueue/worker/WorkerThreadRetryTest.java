package com.jobqueue.worker;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import com.jobqueue.queue.InMemoryJobQueue;
import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.InMemoryJobRepository;
import com.jobqueue.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week6")
class WorkerThreadRetryTest {

    private WorkerThread worker;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (worker != null && worker.isAlive()) {
            worker.requestStop();
            worker.interrupt();
            worker.join(1000);
        }
    }

    @Test
    void jobThatAlwaysFailsIsRetriedMaxRetriesTimesThenDeadLettered() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        AtomicInteger attempts = new AtomicInteger(0);
        int maxRetries = 2;

        registry.registerProcessor("send_email", job -> {
            attempts.incrementAndGet();
            throw new JobProcessingException("always fails");
        });

        worker = new WorkerThread("test-worker", queue, repository, registry, deadLetterQueue, maxRetries);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitDeadLetter(deadLetterQueue, repository, job.getId().toString());

        assertEquals(JobStatus.FAILED, finalState.getStatus());
        assertEquals(maxRetries + 1, attempts.get(), "Processor should be invoked once initially plus once per retry");
        assertEquals(maxRetries + 1, finalState.getRetryCount(), "retryCount should reflect every failed attempt");

        List<Job> deadLetters = deadLetterQueue.peekAll();
        assertEquals(1, deadLetters.size());
        assertEquals(job.getId(), deadLetters.get(0).getId());
    }

    @Test
    void jobThatFailsThenSucceedsCompletesWithoutBeingDeadLettered() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        AtomicInteger attempts = new AtomicInteger(0);
        int maxRetries = 3;

        registry.registerProcessor("send_email", job -> {
            int attemptNumber = attempts.incrementAndGet();
            if (attemptNumber <= 2) {
                throw new JobProcessingException("transient failure #" + attemptNumber);
            }
            return "delivered on attempt " + attemptNumber;
        });

        worker = new WorkerThread("test-worker", queue, repository, registry, deadLetterQueue, maxRetries);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitTerminalStatus(repository, job.getId().toString());

        assertEquals(JobStatus.COMPLETED, finalState.getStatus());
        assertEquals(3, attempts.get(), "Processor should have been called 3 times total");
        assertEquals(2, finalState.getRetryCount(), "retryCount should equal the number of failures, not total attempts");
        assertTrue(deadLetterQueue.isEmpty(), "A job that eventually succeeds must never reach the dead-letter queue");
    }

    @Test
    void zeroMaxRetriesSendsAnyFailureStraightToDeadLetter() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();

        registry.registerProcessor("send_email", job -> {
            throw new JobProcessingException("no retries allowed");
        });

        worker = new WorkerThread("test-worker", queue, repository, registry, deadLetterQueue, 0);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitDeadLetter(deadLetterQueue, repository, job.getId().toString());

        assertEquals(JobStatus.FAILED, finalState.getStatus());
        assertEquals(1, finalState.getRetryCount());
        assertEquals(1, deadLetterQueue.size());
    }

    @Test
    void deadLetterQueueDrainAllRemovesJobsItReturns() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        registry.registerProcessor("send_email", job -> {
            throw new JobProcessingException("fails");
        });

        worker = new WorkerThread("test-worker", queue, repository, registry, deadLetterQueue, 0);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);
        awaitDeadLetter(deadLetterQueue, repository, job.getId().toString());

        assertEquals(1, deadLetterQueue.size());
        List<Job> drained = deadLetterQueue.drainAll();

        assertEquals(1, drained.size());
        assertTrue(deadLetterQueue.isEmpty(), "drainAll() should remove the jobs it returns");
    }

    private Job awaitTerminalStatus(JobRepository repository, String jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            Optional<Job> current = repository.findById(jobId);
            if (current.isPresent()) {
                JobStatus status = current.get().getStatus();
                if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                    return current.get();
                }
            }
            Thread.sleep(10);
        }
        fail("Job " + jobId + " did not reach a terminal status within timeout");
        return null; 
    }

    private Job awaitDeadLetter(DeadLetterQueue deadLetterQueue, JobRepository repository, String jobId)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (!deadLetterQueue.isEmpty()) {
                Optional<Job> current = repository.findById(jobId);
                if (current.isPresent() && current.get().getStatus() == JobStatus.FAILED) {
                    return current.get();
                }
            }
            Thread.sleep(10);
        }
        fail("Job " + jobId + " was not dead-lettered within timeout");
        return null; 
    }
}