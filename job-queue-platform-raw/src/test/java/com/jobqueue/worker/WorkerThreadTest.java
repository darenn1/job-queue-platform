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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week6")
class WorkerThreadTest {

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
    void successfulJobTransitionsFromPendingToRunningToCompleted() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", job -> "delivered:" + job.getId());

        worker = new WorkerThread("test-worker", queue, repository, registry);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitTerminalStatus(repository, job.getId().toString());

        assertEquals(JobStatus.COMPLETED, finalState.getStatus());
        assertEquals("delivered:" + job.getId(), finalState.getResult());
    }

    @Test
    void failedJobIsMarkedFailedWithProcessorMessageAsResult() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", job -> {
            throw new JobProcessingException("simulated failure for " + job.getId());
        });

        worker = new WorkerThread("test-worker", queue, repository, registry);
        worker.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitTerminalStatus(repository, job.getId().toString());

        assertEquals(JobStatus.FAILED, finalState.getStatus());
        assertEquals("simulated failure for " + job.getId(), finalState.getResult());
    }

    @Test
    void unknownJobTypeIsMarkedFailedRatherThanCrashingTheWorker() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry(); 

        worker = new WorkerThread("test-worker", queue, repository, registry);
        worker.start();

        Job job = new Job("totally_unknown_type", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        Job finalState = awaitTerminalStatus(repository, job.getId().toString());

        assertEquals(JobStatus.FAILED, finalState.getStatus());
        assertTrue(worker.isAlive(), "Worker thread must survive an unknown job type, not crash");
    }

    @Test
    void workerProcessesMultipleJobsSequentiallyOnOneThread() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        AtomicInteger processedCount = new AtomicInteger(0);
        registry.registerProcessor("send_email", job -> {
            processedCount.incrementAndGet();
            return "ok";
        });

        worker = new WorkerThread("test-worker", queue, repository, registry);
        worker.start();

        Job[] jobs = new Job[5];
        for (int i = 0; i < 5; i++) {
            jobs[i] = new Job("send_email", "{}", 1);
            repository.save(jobs[i]);
            queue.enqueue(jobs[i]);
        }

        for (Job job : jobs) {
            awaitTerminalStatus(repository, job.getId().toString());
        }

        assertEquals(5, processedCount.get());
    }

    @Test
    void requestStopPreventsPickingUpFurtherJobsOnceIdle() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", job -> "ok");

        worker = new WorkerThread("test-worker", queue, repository, registry);
        worker.start();

       
        Thread.sleep(100);

        worker.requestStop();
        worker.interrupt(); 
        worker.join(1000);

        assertFalse(worker.isAlive(), "Worker should exit after requestStop() + interrupt() while idle");
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

}