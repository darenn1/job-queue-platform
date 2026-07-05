package com.jobqueue.worker;

import com.jobqueue.domain.Job;
import com.jobqueue.domain.JobStatus;
import com.jobqueue.queue.JobQueue;
import com.jobqueue.repository.JobRepository;

public class WorkerThread extends Thread {
  private final JobQueue queue;
  private final JobRepository repository;
  private final ProcessorRegistry registry;
  private volatile boolean running = true;



  public WorkerThread(String name, JobQueue queue, JobRepository repository, ProcessorRegistry registry) {
    super(name);
    this.queue = queue;
    this.repository = repository;
    this.registry = registry;
  }

  public void requestStop() {
    this.running = false;
  }

  public boolean isRunning() {
    return running;
  }

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
    } catch (JobProcessingException e) {
        job.setStatus(JobStatus.FAILED);
        job.setResult(e.getMessage());
    }
 
    repository.update(job);
    }
  }

