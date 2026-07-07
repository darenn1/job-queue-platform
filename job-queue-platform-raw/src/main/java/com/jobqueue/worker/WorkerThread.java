package com.jobqueue.worker;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.JobRepository;

public class WorkerThread extends Thread {

    private final JobQueue queue;
    private final JobRepository repository;
    private final ProcessorRegistry registry;
    private final DeadLetterQueue deadLetterQueue;
    private final int maxRetries;
    private volatile boolean running = true;

    public WorkerThread(String name, JobQueue queue, JobRepository repository, ProcessorRegistry registry) {
        this(name, queue, repository, registry, new DeadLetterQueue(), 0);
    }

    public WorkerThread(String name, JobQueue queue, JobRepository repository, ProcessorRegistry registry,
                         DeadLetterQueue deadLetterQueue, int maxRetries) {
        super(name);
        this.queue = queue;
        this.repository = repository;
        this.registry = registry;
        this.deadLetterQueue = deadLetterQueue;
        this.maxRetries = maxRetries;
    }

    public void requestStop() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (running) {
            Job job;
            try {
                job = queue.dequeue(); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; 
            }

            processJob(job);
        }
    }

    private void processJob(Job job) {
        job.setStatus(JobStatus.RUNNING);
        repository.update(job);

        try {
            JobProcessor processor = registry.getProcessor(job.getType());
            String result = processor.process(job);
            job.setStatus(JobStatus.COMPLETED);
            job.setResult(result);
            repository.update(job);
        } catch (JobProcessingException e) {
            handleFailure(job, e);
        }
    }

    private void handleFailure(Job job, JobProcessingException e) {
        int attemptNumber = job.incrementRetryCount();

        if (attemptNumber <= maxRetries) {
            job.setStatus(JobStatus.PENDING);
            job.setResult(e.getMessage());
            repository.update(job);
            queue.enqueue(job); 
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setResult(e.getMessage());
            repository.update(job);
            deadLetterQueue.add(job);
        }
    }
}