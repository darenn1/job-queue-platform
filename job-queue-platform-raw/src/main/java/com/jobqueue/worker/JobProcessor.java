package com.jobqueue.worker;

import com.jobqueue.domain.Job;

@FunctionalInterface
public interface JobProcessor {

  String process(Job job) throws JobProcessingException;

}
