package com.example.job_queue_platform_refined.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job implements Serializable {
  
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false)
  private String type;
 
  @Column(columnDefinition = "TEXT")
  private String payload;
 
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JobStatus status = JobStatus.PENDING;
 
  @Column(nullable = false)
  private int priority = 0;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "submitted_by")
  private UUID submittedBy;
 
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
 
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
 
  @Column(columnDefinition = "TEXT")
  private String result;

  protected Job() {
      // Default constructor for JPA
  }

  public Job(String type, String payload, int priority) {
      this.type = type;
      this.payload = payload;
      this.priority = priority;
      this.status = JobStatus.PENDING;
  }

  @PrePersist
  protected void onCreate() {
      this.createdAt = Instant.now();
      this.updatedAt = this.createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
      this.updatedAt = Instant.now(); 
  }

  public UUID getId() {
      return id;
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
  }
 
  public int getPriority() {
      return priority;
  }
 
  public void setPriority(int priority) {
      this.priority = priority;
  }

  public int getRetryCount() {
      return retryCount;
  }
 
  public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
  }

  public UUID getSubmittedBy() {
      return submittedBy;
  }
 
  public void setSubmittedBy(UUID submittedBy) {
      this.submittedBy = submittedBy;
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
 
  public void setResult(String result) {
      this.result = result;
  }

  
}
