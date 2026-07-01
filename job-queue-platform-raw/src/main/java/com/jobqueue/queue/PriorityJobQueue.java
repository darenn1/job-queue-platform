package com.jobqueue.queue;

import com.jobqueue.domain.Job;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class PriorityJobQueue implements JobQueue {

    private final BlockingQueue<Job> queue;

    public PriorityJobQueue() {
        this.queue = new PriorityBlockingQueue<>(11, Comparator.comparingInt(Job::getPriority).reversed());
    }

    @Override
    public void enqueue(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        try {
            queue.put(job);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while enqueueing job " + job.getId(), e);
        }
    }

    @Override
    public Job dequeue() throws InterruptedException {
        return queue.take();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }

  
}
