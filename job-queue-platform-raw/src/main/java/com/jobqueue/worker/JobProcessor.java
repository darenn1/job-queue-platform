package com.jobqueue.worker;

import com.jobqueue.domain.Job;

public interface JobProcessor {

  String process(Job job) throws JobProcessingException;

}
