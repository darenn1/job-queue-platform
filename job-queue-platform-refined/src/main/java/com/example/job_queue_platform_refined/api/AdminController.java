package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.AdminJobSummaryRow;
import com.example.job_queue_platform_refined.service.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class AdminController {

    private final JobService jobService;

    public AdminController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/admin/jobs/summary")
    public List<AdminJobSummaryRow> jobsSummary() {
        return jobService.getAdminJobsSummary();
    }
}