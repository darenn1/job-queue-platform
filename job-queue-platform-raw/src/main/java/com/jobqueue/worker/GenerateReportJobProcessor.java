package com.jobqueue.worker;

import com.jobqueue.domain.Job;

import java.util.concurrent.ThreadLocalRandom;

public class GenerateReportJobProcessor implements JobProcessor {

    private final long sleepMillis;
    private final double failureProbability;

    public GenerateReportJobProcessor() {
        this(600, 0.35);
    }

    public GenerateReportJobProcessor(long sleepMillis, double failureProbability) {
        this.sleepMillis = sleepMillis;
        this.failureProbability = failureProbability;
    }

    @Override
    public String process(Job job) throws JobProcessingException {
        simulateReportGeneration();
        if (ThreadLocalRandom.current().nextDouble() < failureProbability) {
            throw new JobProcessingException("Simulated report generation failure for job " + job.getId());
        }
        return "Report generated for job " + job.getId();
    }

    private void simulateReportGeneration() {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobProcessingException("Interrupted while generating report for job", e);
        }
    }
}