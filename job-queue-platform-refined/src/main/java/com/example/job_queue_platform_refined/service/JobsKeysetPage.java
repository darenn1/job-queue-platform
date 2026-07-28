package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.domain.Job;
 
import java.util.List;

public record JobsKeysetPage(List<Job> content, String nextCursor) {
  
}
