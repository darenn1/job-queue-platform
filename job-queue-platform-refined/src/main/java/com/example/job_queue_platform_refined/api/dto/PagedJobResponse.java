package com.example.job_queue_platform_refined.api.dto;

import com.example.job_queue_platform_refined.api.support.JobCursor;
import com.example.job_queue_platform_refined.api.support.JobCursorCodec;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public class PagedJobResponse {
  private final List<JobResponse> content;
  private final int page;
  private final int size;
  private final long totalElements;
  private final int totalPages;
  private final String nextCursor;

  public PagedJobResponse(List<JobResponse> content, int page, int size, long totalElements, int totalPages, String nextCursor) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.nextCursor = nextCursor;
  }

  public static PagedJobResponse from(Page<Job> page) {
    return from(page, null, null, false);
  }

  public static PagedJobResponse from(Page<Job> page, JobStatus status, String type, boolean includeNextCursor) {
    List<JobResponse> content = page.getContent().stream()
        .map(JobResponse::from)
        .toList();
    String nextCursor = nextCursor(page, status, type, includeNextCursor);
    return new PagedJobResponse(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), nextCursor);
  }

  public List<JobResponse> getContent() {
    return content;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  private static String nextCursor(Page<Job> page, JobStatus status, String type, boolean includeNextCursor) {
    if (!includeNextCursor || !page.hasNext() || page.getContent().isEmpty()) {
      return null;
    }

    Job last = page.getContent().get(page.getContent().size() - 1);
    if (last.getCreatedAt() == null || last.getId() == null) {
      return null;
    }

    return JobCursorCodec.encode(new JobCursor(last.getCreatedAt(), last.getId(), status, type));
  }
  
}
