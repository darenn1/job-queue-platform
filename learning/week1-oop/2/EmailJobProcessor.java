import java.util.Random;

public class EmailJobProcessor implements JobProcessor {
  private final Random random = new Random();

  @Override
  public void process(Job job) {
    System.out.println("  [Email] Starting → " + job.getId());

    job.setStatus(JobStatus.IN_PROGRESS);

    try {
      sendEmail(job);
      job.setStatus(JobStatus.DONE);
      System.out.println("  [Email] Completed → " + job.getId());

    } catch (JobProcessingException e) {
      job.setStatus(JobStatus.FAILED);
      System.out.println("  [Email] ✗ Failed: " + e.getMessage());
      System.out.println("          job=" + e.getJobId() + " was in status=" + e.getFailedStatus());
  } finally {
      System.out.println("  [Email] Ending → " + job.getId());
    }
  }
  private void sendEmail(Job job) throws JobProcessingException {
        boolean failed = random.nextDouble() < 0.30;  // 30% failure rate
 
        if (failed) {
            throw new JobProcessingException(
                job.getId(),
                job.getStatus(),
                "SMTP server refused connection"    // captures status at moment of failure
            );
        }
 
        // Simulates work
        System.out.println("  [Email] Sending to payload: " + job.getPayload());
    }
}
