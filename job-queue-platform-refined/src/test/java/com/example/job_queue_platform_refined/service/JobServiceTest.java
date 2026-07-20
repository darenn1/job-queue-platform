package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.repository.JobRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week7")
class JobServiceTest {
 
    @Mock
    private JobRepository jobRepository;
 
    @Test
    void submitJobSavesAndReturnsTheJob() {
        JobService service = new JobService(jobRepository);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
 
        Job result = service.submitJob("send_email", "{\"to\":\"a@b.com\"}", 2);
 
        assertEquals("send_email", result.getType());
        assertEquals(2, result.getPriority());
        assertEquals(JobStatus.PENDING, result.getStatus());
        verify(jobRepository, times(1)).save(any(Job.class));
    }
 
    @Test
    void getJobReturnsJobWhenFound() {
        JobService service = new JobService(jobRepository);
        UUID id = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
 
        Job result = service.getJob(id);
 
        assertEquals("resize_image", result.getType());
    }
 
    @Test
    void getJobThrowsJobNotFoundExceptionWhenAbsent() {
        JobService service = new JobService(jobRepository);
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());
 
        JobNotFoundException ex = assertThrows(JobNotFoundException.class, () -> service.getJob(id));
        assertTrue(ex.getMessage().contains(id.toString()),
                "exception message should include the id that wasn't found, useful for debugging/logs");
    }
 
    @Test
    void listJobsDelegatesToFindByStatusWhenStatusGiven() {
        JobService service = new JobService(jobRepository);
        Job failed = new Job("generate_report", "{}", 0);
        failed.setStatus(JobStatus.FAILED);
        when(jobRepository.findByStatus(JobStatus.FAILED)).thenReturn(List.of(failed));
 
        List<Job> result = service.listJobs(JobStatus.FAILED);
 
        assertEquals(1, result.size());
        verify(jobRepository).findByStatus(JobStatus.FAILED);
        verify(jobRepository, never()).findAll();
    }
 
    @Test
    void listJobsDelegatesToFindAllWhenNoStatusGiven() {
        JobService service = new JobService(jobRepository);
        when(jobRepository.findAll()).thenReturn(List.of(new Job("send_email", "{}", 0)));
 
        List<Job> result = service.listJobs(null);
 
        assertEquals(1, result.size());
        verify(jobRepository).findAll();
        verify(jobRepository, never()).findByStatus(any());
    }
}
 
