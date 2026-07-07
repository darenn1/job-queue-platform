package com.jobqueue.stress;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import com.jobqueue.queue.InMemoryJobQueue;
import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.InMemoryJobRepository;
import com.jobqueue.repository.JobRepository;
import com.jobqueue.worker.DeadLetterQueue;
import com.jobqueue.worker.EmailJobProcessor;
import com.jobqueue.worker.GenerateReportJobProcessor;
import com.jobqueue.worker.ProcessorRegistry;
import com.jobqueue.worker.ResizeImageJobProcessor;
import com.jobqueue.worker.WorkerPool;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class StressTest {

    private static final int WORKER_COUNT = 4;
    private static final int JOB_COUNT = 200;
    private static final long TERMINAL_WAIT_TIMEOUT_SECONDS = 60;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private StressTest() {
    }

    public static void main(String[] args) throws InterruptedException {
        log("Starting Phase 1 stress test: %d workers, %d jobs", WORKER_COUNT, JOB_COUNT);

        JobQueue queue = new InMemoryJobQueue();
        JobRepository repository = new InMemoryJobRepository();
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();

        ProcessorRegistry registry = new ProcessorRegistry();
        registry.registerProcessor("send_email", new EmailJobProcessor());
        registry.registerProcessor("resize_image", new ResizeImageJobProcessor());
        registry.registerProcessor("generate_report", new GenerateReportJobProcessor());

        WorkerPool pool = new WorkerPool(WORKER_COUNT, queue, repository, registry, deadLetterQueue, 2);
        pool.start();

        List<Job> submittedJobs = submitMixedJobs(queue, repository, JOB_COUNT);
        log("Submitted %d jobs of mixed types", submittedJobs.size());

        long startTime = System.currentTimeMillis();
        awaitAllTerminal(repository, submittedJobs, TERMINAL_WAIT_TIMEOUT_SECONDS);
        long elapsedMillis = System.currentTimeMillis() - startTime;
        log("All jobs reached a terminal status in %d ms", elapsedMillis);

        boolean cleanShutdown = pool.shutdown(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log("Worker pool shutdown %s", cleanShutdown ? "completed cleanly" : "TIMED OUT (forced)");

        int completedCount = repository.findByStatus(JobStatus.COMPLETED).size();
        int failedCount = repository.findByStatus(JobStatus.FAILED).size();
        int deadLetterCount = deadLetterQueue.size();

        log("Results: COMPLETED=%d, FAILED=%d, dead-lettered=%d", completedCount, failedCount, deadLetterCount);

        int totalAccountedFor = completedCount + failedCount;
        if (totalAccountedFor != JOB_COUNT) {
            fail("Expected COMPLETED + FAILED to equal " + JOB_COUNT + " but got " + totalAccountedFor
                    + " — jobs were lost or are stuck in a non-terminal state.");
        }
        if (failedCount != deadLetterCount) {
            fail("Expected every FAILED job to have a matching dead-letter entry: FAILED=" + failedCount
                    + " but dead-letter size=" + deadLetterCount);
        }
        if (!cleanShutdown) {
            fail("Worker pool did not shut down cleanly within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds.");
        }

        log("STRESS TEST PASSED: %d jobs in, %d COMPLETED + %d FAILED = %d out, none lost.",
                JOB_COUNT, completedCount, failedCount, totalAccountedFor);
    }

    private static List<Job> submitMixedJobs(JobQueue queue, JobRepository repository, int count) {
        String[] jobTypes = {"send_email", "resize_image", "generate_report"};
        List<Job> jobs = new java.util.ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String type = jobTypes[ThreadLocalRandom.current().nextInt(jobTypes.length)];
            int priority = ThreadLocalRandom.current().nextInt(1, 6);
            Job job = new Job(type, "{\"index\":" + i + "}", priority);

            repository.save(job);
            queue.enqueue(job);
            jobs.add(job);
        }
        return jobs;
    }

    private static void awaitAllTerminal(JobRepository repository, List<Job> jobs, long timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);

        for (Job job : jobs) {
            String jobId = job.getId().toString();
            while (System.currentTimeMillis() < deadline) {
                var current = repository.findById(jobId);
                if (current.isPresent()) {
                    JobStatus status = current.get().getStatus();
                    if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                        break;
                    }
                }
                Thread.sleep(20);
            }
            if (System.currentTimeMillis() >= deadline) {
                fail("Timed out after " + timeoutSeconds + "s waiting for job " + jobId + " to reach a terminal status");
            }
        }
    }

    private static void log(String format, Object... args) {
        System.out.printf("[StressTest] " + format + "%n", args);
    }

    private static void fail(String message) {
        System.err.println("[StressTest] FAILURE: " + message);
        System.exit(1);
    }
}