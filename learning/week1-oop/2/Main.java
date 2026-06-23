import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job("EMAIL",  "{\"to\":\"alice@example.com\",\"subject\":\"Welcome\"}"));
        jobs.add(new Job("REPORT", "{\"reportId\":\"q3-summary\",\"format\":\"PDF\"}"));
        jobs.add(new Job("EMAIL",  "{\"to\":\"bob@example.com\",\"subject\":\"Invoice\"}"));
        jobs.add(new Job("REPORT", "{\"reportId\":\"monthly-sales\",\"format\":\"PDF\"}"));
        jobs.add(new Job("EMAIL",  "{\"to\":\"carol@example.com\",\"subject\":\"Reset\"}"));
        jobs.add(new Job("EMAIL",  "{\"to\":\"dave@example.com\",\"subject\":\"Confirm\"}"));

        EmailJobProcessor emailJobProcessor = new EmailJobProcessor();
        ReportJobProcessor reportJobProcessor = new ReportJobProcessor();

        JobProcessor emailProcessor = job -> emailJobProcessor.process(job);
        JobProcessor reportProcessor = job -> reportJobProcessor.process(job);

        List<JobProcessor> processors = new ArrayList<>();
        processors.add(emailProcessor);
        processors.add(reportProcessor);
        processors.add(emailProcessor);
        processors.add(reportProcessor);
        processors.add(emailProcessor);
        processors.add(emailProcessor);

        System.out.println(" Job Queue Starting " + jobs.size() + " jobs \n");

        int completed = 0;
        int failed = 0;

        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);

            System.out.println("Job #" + (i + 1)
                    + " | type=" + job.getType()
                    + " | status=" + job.getStatus());

            processors.get(i).process(job);

            if (job.getStatus() == JobStatus.DONE) {
                completed++;
            } else if (job.getStatus() == JobStatus.FAILED) {
                failed++;
            }

            System.out.println();
        }

        System.out.println("Summary");
        System.out.println("Completed : " + completed);
        System.out.println("Failed    : " + failed);
        System.out.println();

        List<String> failedJobIds = jobs.stream()
                .filter(job -> job.getStatus() == JobStatus.FAILED)
                .map(Job::getId)
                .collect(Collectors.toList());

        System.out.println("Failed job IDs:");
        System.out.println(failedJobIds);
        System.out.println();

        Optional<Job> possibleJob = findJobById(jobs, jobs.get(0).getId());

        possibleJob.ifPresent(job -> {
            System.out.println("Found job by ID: " + job.getId());
        });

        System.out.println();

        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);

            String icon = switch (job.getStatus()) {
                case DONE -> "DONE";
                case FAILED -> "FAILED";
                case IN_PROGRESS -> "IN_PROGRESS";
                case PENDING -> "PENDING";
            };

            System.out.println(icon + " Job #" + (i + 1)
                    + " [" + job.getStatus().name() + "] "
                    + job.getId());
        }
    }

    public static Optional<Job> findJobById(List<Job> jobs, String id) {
        return jobs.stream()
                .filter(job -> job.getId().equals(id))
                .findFirst();
    }
}