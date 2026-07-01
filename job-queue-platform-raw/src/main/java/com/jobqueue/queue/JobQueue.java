package com.jobqueue.queue;

import com.jobqueue.domain.Job;

public interface JobQueue {

  void enqueue(Job job);
    // Implementation for adding a job to the queue

  Job dequeue() throws InterruptedException;
    // Implementation for removing and returning a job from the queue

  boolean isEmpty();
    // Implementation for checking if the queue is empty    

  int size();
    // Implementation for getting the number of jobs in the queue
  
}
