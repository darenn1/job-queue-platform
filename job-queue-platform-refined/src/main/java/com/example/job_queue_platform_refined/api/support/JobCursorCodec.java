package com.example.job_queue_platform_refined.api.support;

import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class JobCursorCodec {
  private static final String DELIMITER = "\\|";
 
  public static String encode(JobCursor cursor) {
      String raw = String.join("|",
              String.valueOf(cursor.lastCreatedAt().toEpochMilli()),
              cursor.lastId().toString(),
              cursor.status() != null ? cursor.status().name() : "",
              cursor.type() != null ? cursor.type() : ""
      );
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
  public static JobCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split(DELIMITER, -1); // -1 keeps trailing empty fields
            if (parts.length != 4) {
                throw new IllegalArgumentException("expected 4 fields, got " + parts.length);
            }
 
            Instant lastCreatedAt = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            UUID lastId = UUID.fromString(parts[1]);
            JobStatus status = parts[2].isEmpty() ? null : JobStatus.valueOf(parts[2]);
            String type = parts[3].isEmpty() ? null : parts[3];
 
            return new JobCursor(lastCreatedAt, lastId, status, type);
 
        } catch (Exception ex) {
            throw new InvalidCursorException(encoded, ex);
        }
    }
}
