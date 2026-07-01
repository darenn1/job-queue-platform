package com.jobqueue.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;
import java.util.UUID;
import java.time.Instant;

public class Job {
  private UUID id;
  private String type; 
  private String payload;
  private JobStatus status;
  private int priority;
  private Instant createdAt;
  private Instant updatedAt;
  private String result;

  public Job() {

  }

  public Job(String type, String payload, int priority) {
    this.id = UUID.randomUUID();
    this.type = type;
    this.payload = payload;
    this.priority = priority;
    this.status = JobStatus.PENDING;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  @JsonCreator
  public Job(@JsonProperty("id") UUID id,
            @JsonProperty("type") String type,
             @JsonProperty("payload") String payload,
             @JsonProperty("status") JobStatus status,
             @JsonProperty("priority") int priority,
             @JsonProperty("createdAt") Instant createdAt,
             @JsonProperty("updatedAt") Instant updatedAt,
             @JsonProperty("result") String result) {
    this.id = id;
    this.type = type;
    this.payload = payload;
    this.status = status;
    this.priority = priority;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.result = result;
  }

  public UUID getId() {
    return id;
  } 

  public void setId(UUID id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public JobStatus getStatus() {
    return status;
  }

  public void setStatus(JobStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Job)) return false;
    Job job = (Job) o;
    return Objects.equals(id, job.id) ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);  
  }

  @Override
  public String toString() {
    return "Job{" +
            ", id=" + id +
            "type='" + type + '\'' + 
            ", status=" + status +
            ", priority=" + priority +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
  }

  
}
