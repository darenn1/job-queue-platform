package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.api.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
 
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class JobController {

  private final JobRepository jobRepository;

  public JobController(JobRepository jobRepository) {
      this.jobRepository = jobRepository;
  }

  @PostMapping("/jobs")
  public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest request) {
    Job job = new Job(request.getType(), request.getPayload(), request.getPriority());
    Job saved = jobRepository.save(job);
    return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(saved));
  }

  @GetMapping("/jobs/{id}")
  public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
    Job job = jobRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found " + id));
    return ResponseEntity.ok(JobResponse.from(job));
  }

  @GetMapping("/jobs")
  public List<JobResponse> listJobs(@RequestParam(required = false) JobStatus status) {
        List<Job> jobs = (status != null)
                ? jobRepository.findByStatus(status)
                : jobRepository.findAll();
 
        return jobs.stream()
                .map(JobResponse::from)
                .collect(Collectors.toList());
  }
  
}
