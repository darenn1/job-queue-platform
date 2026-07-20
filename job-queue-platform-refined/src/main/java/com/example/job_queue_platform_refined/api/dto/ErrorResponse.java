package com.example.job_queue_platform_refined.api.dto;
import java.time.Instant;

public class ErrorResponse {
 
    private final String error;
    private final String field;
    private final Instant timestamp;
 
    public ErrorResponse(String error, String field, Instant timestamp) {
        this.error = error;
        this.field = field;
        this.timestamp = timestamp;
    }
 
    public String getError() {
        return error;
    }
 
    public String getField() {
        return field;
    }
 
    public Instant getTimestamp() {
        return timestamp;
    }
}
