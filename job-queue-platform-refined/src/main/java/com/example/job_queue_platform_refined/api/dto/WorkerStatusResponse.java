package com.example.job_queue_platform_refined.api.dto;

public class WorkerStatusResponse {

    private final int activeWorkers;
    private final long jobsRunning;
    private final long queueDepth;
    private final long totalCompleted;
    private final long totalFailed;

    public WorkerStatusResponse(int activeWorkers, long jobsRunning, long queueDepth,
                                 long totalCompleted, long totalFailed) {
        this.activeWorkers = activeWorkers;
        this.jobsRunning = jobsRunning;
        this.queueDepth = queueDepth;
        this.totalCompleted = totalCompleted;
        this.totalFailed = totalFailed;
    }

    public int getActiveWorkers() {
        return activeWorkers;
    }

    public long getJobsRunning() {
        return jobsRunning;
    }

    public long getQueueDepth() {
        return queueDepth;
    }

    public long getTotalCompleted() {
        return totalCompleted;
    }

    public long getTotalFailed() {
        return totalFailed;
    }
}