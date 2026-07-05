package com.jobqueue.worker;

import com.jobqueue.domain.Job;

import java.util.concurrent.ThreadLocalRandom;

public class ResizeImageJobProcessor implements JobProcessor {

    private final long sleepMillis;
    private final double failureProbability;

    public ResizeImageJobProcessor() {
        this(400, 0.15);
    }

    public ResizeImageJobProcessor(long sleepMillis, double failureProbability) {
        this.sleepMillis = sleepMillis;
        this.failureProbability = failureProbability;
    }

    @Override
    public String process(Job job) throws JobProcessingException {
        simulateImageProcessing();
        if (ThreadLocalRandom.current().nextDouble() < failureProbability) {
            throw new JobProcessingException("Simulated image resize failure for job " + job.getId());
        }
        return "Image resized for job " + job.getId();
    }

    private void simulateImageProcessing() {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobProcessingException("Interrupted while resizing image for job", e);
        }
    }
}