package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.api.dto.JobResponse;
import com.example.job_queue_platform_refined.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class JobController {

  private final JobService jobService;

  public JobController(JobService jobService) {
      this.jobService = jobService;
  }

  @PostMapping("/jobs")
  public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest request) {
    Job saved = jobService.submitJob(request.getType(), request.getPayload(), request.getPriority());
    return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(saved));
  }

  @GetMapping("/jobs/{id}")
  public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
    Job job = jobService.getJob(id);
    return ResponseEntity.ok(JobResponse.from(job));
  }

  @GetMapping("/jobs")
  public List<JobResponse> listJobs(@RequestParam(required = false) JobStatus status) {
      return jobService.listJobs(status).stream()
              .map(JobResponse::from)
              .collect(Collectors.toList());
  }
  
}
