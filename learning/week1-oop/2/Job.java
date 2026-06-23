import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
public class Job {
  private final String id;
  private String type;
  private String payload;
  private JobStatus status;
  private final LocalDateTime createdAt;

  public Job(String type, String payload) {
    this.id = UUID.randomUUID().toString();
    this.type = type;
    this.payload = payload;
    this.status = JobStatus.PENDING;
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }
  public String getType() {
    return type;
  }
  public String getPayload() {
    return payload; 
  }
  public JobStatus getStatus() {
    return status;
  }
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setStatus(JobStatus status) {
    if (status == null || type.isEmpty()) {
      throw new IllegalArgumentException("Status cannot be null or empty");
    }
    this.status = status;
  }

  public void setType(String type) {
    if (type == null || type.isEmpty()) {
      throw new IllegalArgumentException("Type cannot be null or empty");
    }
    this.type = type;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  @Override
  public String toString() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return String.format("Job{id='%s', type='%s', payload='%s', status='%s', createdAt='%s'}",
            id, type, payload, status, createdAt.format(formatter));
  }

}
