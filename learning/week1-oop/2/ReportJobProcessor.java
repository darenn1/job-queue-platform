import java.util.Random;

public class ReportJobProcessor implements JobProcessor {

    private static final Random RANDOM = new Random();

    @Override
    public void process(Job job) {
        System.out.println("  [Report] Starting → " + job.getId());

        job.setStatus(JobStatus.IN_PROGRESS);

        try {
            generateReport(job);
            job.setStatus(JobStatus.DONE);
            System.out.println("  [Report] ✓ Completed");

        } catch (JobProcessingException e) {
            job.setStatus(JobStatus.FAILED);
            System.out.println("  [Report] ✗ Failed: " + e.getMessage());

        } finally {
            System.out.println("  [Report] finally block — cleanup done. Current status: " + job.getStatus());
        }
    }

    private void generateReport(Job job) throws JobProcessingException {
        boolean failed = RANDOM.nextDouble() < 0.30;

        if (failed) {
            throw new JobProcessingException(
                job.getId(),
                job.getStatus(),
                "Report engine ran out of memory"
            );
        }

        System.out.println("  [Report] Generating PDF for payload: " + job.getPayload());
    }
}