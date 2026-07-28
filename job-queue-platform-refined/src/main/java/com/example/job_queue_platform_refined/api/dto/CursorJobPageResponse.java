package com.example.job_queue_platform_refined.api.dto;

import java.util.List;

public class CursorJobPageResponse {
  private final List<JobResponse> content;
    private final String nextCursor;
 
  public CursorJobPageResponse(List<JobResponse> content, String nextCursor) {
       this.content = content;
      this.nextCursor = nextCursor;
  }
 
  public List<JobResponse> getContent() {
      return content;
  }
 
  public String getNextCursor() {
      return nextCursor;
  }
  
}
