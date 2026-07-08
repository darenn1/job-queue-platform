package com.example.job_queue_platform_refined.api.dto;

import jakarta.validation.constraints.NotBlank;
 
public class SubmitJobRequest {
 
  @NotBlank(message = "type must not be blank")
  private String type;
 
  private String payload;
 
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
 