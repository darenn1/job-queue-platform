package com.example.job_queue_platform_refined.worker;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("week7")
class WorkerMetricsTest {

    @Test
    void jobStartedIncrementsRunningCount() {
        WorkerMetrics metrics = new WorkerMetrics();

        metrics.jobStarted();
        metrics.jobStarted();

        assertEquals(2, metrics.getJobsRunning());
        assertEquals(0, metrics.getTotalCompleted());
        assertEquals(0, metrics.getTotalFailed());
    }

    @Test
    void jobCompletedDecrementsRunningAndIncrementsCompleted() {
        WorkerMetrics metrics = new WorkerMetrics();
        metrics.jobStarted();

        metrics.jobCompleted();

        assertEquals(0, metrics.getJobsRunning());
        assertEquals(1, metrics.getTotalCompleted());
    }

    @Test
    void jobFailedDecrementsRunningAndIncrementsFailed() {
        WorkerMetrics metrics = new WorkerMetrics();
        metrics.jobStarted();

        metrics.jobFailed();

        assertEquals(0, metrics.getJobsRunning());
        assertEquals(1, metrics.getTotalFailed());
    }

    @Test
    void mixedSequenceTracksAllThreeCountersIndependently() {
        WorkerMetrics metrics = new WorkerMetrics();

        metrics.jobStarted();
        metrics.jobStarted(); 
        metrics.jobCompleted(); 
        metrics.jobStarted();
        metrics.jobFailed(); 

        assertEquals(1, metrics.getJobsRunning());
        assertEquals(1, metrics.getTotalCompleted());
        assertEquals(1, metrics.getTotalFailed());
    }
}