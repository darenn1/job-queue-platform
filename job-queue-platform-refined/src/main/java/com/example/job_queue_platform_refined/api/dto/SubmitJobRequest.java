package com.example.job_queue_platform_refined.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
 
public class SubmitJobRequest {
 
  @NotBlank(message = "type must not be blank")
  @Size(max = 100, message = "type must be at most 100 characters")
  private String type;
 
  private String payload;
 
  @Min(value = 0, message = "priority must not be negative")
  private int priority = 0;
 
  public SubmitJobRequest() {
  }
 
  public SubmitJobRequest(String type, String payload, int priority) {
      this.type = type;
      this.payload = payload;
      this.priority = priority;
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
 
  public int getPriority() {
      return priority;
  }
 
  public void setPriority(int priority) {
      this.priority = priority;
  }
}
 