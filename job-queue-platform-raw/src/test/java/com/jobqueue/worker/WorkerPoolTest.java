package com.jobqueue.worker;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week6")
class WorkerPoolTest {

    @Test
    void startLaunchesExactlyPoolSizeThreads() {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", job -> "ok");

        WorkerPool pool = new WorkerPool(4, queue, repository, registry);
        pool.start();

        try {
            assertEquals(4, pool.size());
            for (WorkerThread worker : pool.getWorkers()) {
                assertTrue(worker.isAlive());
            }
        } finally {
            shutdownQuietly(pool);
        }
    }

    @Test
    void startingAPoolTwiceThrows() {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();

        WorkerPool pool = new WorkerPool(2, queue, repository, registry);
        pool.start();

        try {
            assertThrows(IllegalStateException.class, pool::start);
        } finally {
            shutdownQuietly(pool);
        }
    }

    @Test
    void poolProcessesAllSubmittedJobsWithNoneLost() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", job -> "ok");

        WorkerPool pool = new WorkerPool(4, queue, repository, registry);
        pool.start();

        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Job job = new Job("send_email", "{}", 1);
            repository.save(job);
            queue.enqueue(job);
            jobs.add(job);
        }

        try {
            for (Job job : jobs) {
                awaitTerminalStatus(repository, job.getId().toString());
            }
            long completed = repository.findByStatus(JobStatus.COMPLETED).size();
            assertEquals(50, completed, "Every submitted job should reach COMPLETED with none lost");
        } finally {
            shutdownQuietly(pool);
        }
    }

    @Test
    void shutdownWaitsForInFlightJobToFinishBeforeReturning() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();
        CountDownLatch processingStarted = new CountDownLatch(1);
        CountDownLatch letProcessingFinish = new CountDownLatch(1);

        registry.registerProcessor("send_email", job -> {
            processingStarted.countDown();
            try {
                letProcessingFinish.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });

        WorkerPool pool = new WorkerPool(1, queue, repository, registry);
        pool.start();

        Job job = new Job("send_email", "{}", 1);
        repository.save(job);
        queue.enqueue(job);

        assertTrue(processingStarted.await(2, TimeUnit.SECONDS), "Processor should have started");

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            letProcessingFinish.countDown();
        }).start();

        boolean cleanShutdown = pool.shutdown(3, TimeUnit.SECONDS);

        assertTrue(cleanShutdown, "Shutdown should complete cleanly within the timeout");
        assertEquals(JobStatus.COMPLETED, repository.findById(job.getId().toString()).orElseThrow().getStatus(),
                "The in-flight job must finish (not be abandoned) before shutdown() returns");
    }

    @Test
    void shutdownInterruptsIdleWorkersRatherThanBlockingForever() throws InterruptedException {
        JobQueue queue = new InMemoryJobQueue(); 
        JobRepository repository = new InMemoryJobRepository();
        ProcessorRegistry registry = new ProcessorRegistry();

        WorkerPool pool = new WorkerPool(3, queue, repository, registry);
        pool.start();

        Thread.sleep(100); 

        boolean cleanShutdown = pool.shutdown(2, TimeUnit.SECONDS);

        assertTrue(cleanShutdown, "Idle workers should be interrupted and exit promptly, not time out");
        for (WorkerThread worker : pool.getWorkers()) {
            assertFalse(worker.isAlive());
        }
    }

    private void awaitTerminalStatus(JobRepository repository, String jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            var current = repository.findById(jobId);
            if (current.isPresent()) {
                JobStatus status = current.get().getStatus();
                if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                    return;
                }
            }
            Thread.sleep(10);
        }
        fail("Job " + jobId + " did not reach a terminal status within timeout");
    }

    private void shutdownQuietly(WorkerPool pool) {
        try {
            pool.shutdown(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}