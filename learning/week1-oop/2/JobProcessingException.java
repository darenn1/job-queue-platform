public class JobProcessingException extends RuntimeException {
  private final String jobId;
  private final JobStatus failedStatus;

  public JobProcessingException(String jobId, JobStatus failedStatus, String message) {
    super(message);
    this.jobId = jobId;
    this.failedStatus = failedStatus;
  }

  public JobProcessingException(String jobId, JobStatus failedStatus, String message, Throwable cause) {
    super(message, cause);
    this.jobId = jobId;
    this.failedStatus = failedStatus;
  }

  public String getJobId() {
    return jobId;
  }
  public JobStatus getFailedStatus() {
    return failedStatus;
  }
  
}
