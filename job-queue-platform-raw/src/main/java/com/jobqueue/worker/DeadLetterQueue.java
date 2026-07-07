package com.jobqueue.worker;

import com.jobqueue.domain.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DeadLetterQueue {

  private final BlockingQueue<Job> DLQ = new LinkedBlockingQueue<>();

  public void add(Job job) {
    DLQ.offer(job);
  }

  public int size() {
    return DLQ.size();
  }

  public boolean isEmpty() {
    return DLQ.isEmpty();
  }

  public List<Job> peekAll() {
    return new ArrayList<>(DLQ);
  }

  public List<Job> drainAll() {
    List<Job> jobs = new ArrayList<>();
    DLQ.drainTo(jobs);
    return jobs;
  }


  
}
