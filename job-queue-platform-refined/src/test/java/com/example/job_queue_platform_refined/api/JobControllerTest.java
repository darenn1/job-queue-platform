package com.example.job_queue_platform_refined.api;

import tools.jackson.databind.json.JsonMapper;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
import static org.mockito.ArgumentMatchers.any;
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
    private JobRepository jobRepository;
 
    @Test
    void submitJob_persistsAndReturns201() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("send_email", "{\"to\":\"a@b.com\"}", 3);
 
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
 
        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("send_email"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value(3));
 
        verify(jobRepository, times(1)).save(any(Job.class));
    }
 
    @Test
    void submitJob_rejectsBlankType() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("", "{}", 0);
 
        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
 
        verifyNoInteractions(jobRepository);
    }
 
    @Test
    void getJob_returns200WhenFound() throws Exception {
        Job job = new Job("resize_image", "{}", 0);
        UUID id = UUID.randomUUID();
        setId(job, id);
 
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
 
        mockMvc.perform(get("/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("resize_image"));
    }
 
    @Test
    void getJob_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());
 
        mockMvc.perform(get("/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }
 
    @Test
    void listJobs_filtersByStatusWhenQueryParamProvided() throws Exception {
        Job failed = new Job("generate_report", "{}", 0);
        failed.setStatus(JobStatus.FAILED);
        when(jobRepository.findByStatus(JobStatus.FAILED)).thenReturn(List.of(failed));
 
        mockMvc.perform(get("/jobs").queryParam("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
 
        verify(jobRepository).findByStatus(JobStatus.FAILED);
        verify(jobRepository, never()).findAll();
    }
 
    @Test
    void listJobs_returnsAllWhenNoStatusProvided() throws Exception {
        when(jobRepository.findAll()).thenReturn(List.of(
                new Job("send_email", "{}", 0),
                new Job("resize_image", "{}", 0)
        ));
 
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
 
        verify(jobRepository).findAll();
        verify(jobRepository, never()).findByStatus(any());
    }
 
    private static void setId(Job job, UUID id) throws Exception {
        var field = Job.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(job, id);
    }
}
 
