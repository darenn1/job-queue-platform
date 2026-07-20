package com.example.job_queue_platform_refined.exception;

import com.example.job_queue_platform_refined.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
 
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(JobNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleJobNotFound(JobNotFoundException ex) {
      ErrorResponse body = new ErrorResponse(ex.getMessage(), null, Instant.now());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
      FieldError fieldError = ex.getBindingResult().getFieldError();
      String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation error";
      String field = fieldError != null ? fieldError.getField() : null;

      ErrorResponse body = new ErrorResponse(message, field, Instant.now());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
      ErrorResponse body = new ErrorResponse("Internal Server Error", null, Instant.now());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

}
