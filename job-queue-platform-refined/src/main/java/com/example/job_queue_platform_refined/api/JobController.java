package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.CursorJobPageResponse;
import com.example.job_queue_platform_refined.api.dto.JobResponse;
import com.example.job_queue_platform_refined.api.dto.PagedJobResponse;
import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.service.JobService;
import com.example.job_queue_platform_refined.service.JobsKeysetPage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody SubmitJobRequest request,
                                                  @AuthenticationPrincipal User currentUser) {
        Job saved = jobService.submitJob(request.getType(), request.getPayload(), request.getPriority(),
                currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(saved));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        Job job = jobService.getJob(id, currentUser.getId());
        return ResponseEntity.ok(JobResponse.from(job));
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> listJobs(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        UUID currentUserId = currentUser.getId();

        if (after != null) {
            JobsKeysetPage keysetPage = jobService.listJobsKeyset(currentUserId, status, type, after, size);
            var content = keysetPage.content().stream().map(JobResponse::from).toList();
            return ResponseEntity.ok(new CursorJobPageResponse(content, keysetPage.nextCursor()));
        }

        Sort.Direction direction = sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = sort.split(",")[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Job> result = jobService.listJobsOffset(currentUserId, status, type, pageable);
        return ResponseEntity.ok(PagedJobResponse.from(result, status, type, true));
    }
}
