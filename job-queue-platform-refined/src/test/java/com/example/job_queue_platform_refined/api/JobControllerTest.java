package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.service.JobService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
 
import java.util.List;
import java.util.UUID;
 
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("week7")
class JobControllerTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @Autowired
    private JsonMapper jsonMapper;
 
    @MockitoBean
    private JobService jobService;
 
    @Test
    void submitJob_persistsAndReturns201() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("send_email", "{\"to\":\"a@b.com\"}", 3);
        Job saved = new Job("send_email", "{\"to\":\"a@b.com\"}", 3);
 
        when(jobService.submitJob(eq("send_email"), eq("{\"to\":\"a@b.com\"}"), eq(3))).thenReturn(saved);
 
        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("send_email"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value(3));
 
        verify(jobService, times(1)).submitJob("send_email", "{\"to\":\"a@b.com\"}", 3);
    }
 
    @Test
    void submitJob_rejectsBlankType_withStructuredErrorBody() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("", "{}", 0);
 
        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // Day 35: assert on the actual GlobalExceptionHandler response
                // shape, not just the status code.
                .andExpect(jsonPath("$.field").value("type"))
                .andExpect(jsonPath("$.error").value("type must not be blank"))
                .andExpect(jsonPath("$.timestamp").exists());
 
        verifyNoInteractions(jobService);
    }
 
    @Test
    void submitJob_rejectsNegativePriority() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("send_email", "{}", -1);
 
        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("priority"));
 
        verifyNoInteractions(jobService);
    }
 
    @Test
    void getJob_returns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        when(jobService.getJob(id)).thenReturn(job);
 
        mockMvc.perform(get("/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("resize_image"));
    }
 
    @Test
    void getJob_returns404WithStructuredErrorBody_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getJob(id)).thenThrow(new JobNotFoundException(id));
 
        mockMvc.perform(get("/jobs/{id}", id))
                .andExpect(status().isNotFound())
                // Day 35: JobNotFoundException -> GlobalExceptionHandler -> this body.
                .andExpect(jsonPath("$.error").value("Job not found: " + id))
                .andExpect(jsonPath("$.field").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }
 
    @Test
    void listJobs_filtersByStatusWhenQueryParamProvided() throws Exception {
        Job failed = new Job("generate_report", "{}", 0);
        failed.setStatus(JobStatus.FAILED);
        when(jobService.listJobs(JobStatus.FAILED)).thenReturn(List.of(failed));
 
        mockMvc.perform(get("/jobs").queryParam("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
 
        verify(jobService).listJobs(JobStatus.FAILED);
    }
 
    @Test
    void listJobs_returnsAllWhenNoStatusProvided() throws Exception {
        when(jobService.listJobs(null)).thenReturn(List.of(
                new Job("send_email", "{}", 0),
                new Job("resize_image", "{}", 0)
        ));
 
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
 
        verify(jobService).listJobs(null);
    }
}