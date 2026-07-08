package com.example.job_queue_platform_refined.api.dto;

import com.example.job_queue_platform_refined.domain.Job;
 
import java.time.Instant;
import java.util.UUID;

public class JobResponse {
 
  private UUID id;
  private String type;
  private String payload;
  private String status;
  private int priority;
  private Instant createdAt;
  private Instant updatedAt;
  private String result;
 
  public static JobResponse from(Job job) {
      JobResponse r = new JobResponse();
      r.id = job.getId();
      r.type = job.getType();
      r.payload = job.getPayload();
      r.status = job.getStatus().name();
      r.priority = job.getPriority();
      r.createdAt = job.getCreatedAt();
      r.updatedAt = job.getUpdatedAt();
      r.result = job.getResult();
      return r;
  }
 
  public UUID getId() {
      return id;
  }
 
  public String getType() {
      return type;
  }
 
  public String getPayload() {
      return payload;
  }
 
  public String getStatus() {
      return status;
  }
 
  public int getPriority() {
      return priority;
  }
 
  public Instant getCreatedAt() {
      return createdAt;
  }
 
  public Instant getUpdatedAt() {
      return updatedAt;
  }
 
  public String getResult() {
      return result;
  }
}
 