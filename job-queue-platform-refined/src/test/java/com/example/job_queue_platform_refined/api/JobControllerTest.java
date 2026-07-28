package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.JobNotFoundException;
import com.example.job_queue_platform_refined.security.JwtAuthenticationFilter;
import com.example.job_queue_platform_refined.service.JobService;
import com.example.job_queue_platform_refined.service.JobsKeysetPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@WebMvcTest(
        controllers = {JobController.class, AdminController.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Tag("week8_5")
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
        when(jobService.submitJob("send_email", "{\"to\":\"a@b.com\"}", 3)).thenReturn(saved);

        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("send_email"));
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
    void getJob_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getJob(id)).thenThrow(new JobNotFoundException(id));

        mockMvc.perform(get("/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }


    @Test
    void listJobs_defaultsToOffsetMode_returnsPagedEnvelope() throws Exception {
        Job job = new Job("send_email", "{}", 0);
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(jobService.listJobsOffset(eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(job), expectedPageable, 847));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(847))
                .andExpect(jsonPath("$.totalPages").value(43));
    }

    @Test
    void listJobs_offsetMode_honorsPageAndSizeParams() throws Exception {
        Pageable expectedPageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(jobService.listJobsOffset(eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

        mockMvc.perform(get("/jobs").queryParam("page", "2").queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void listJobs_offsetMode_passesStatusAndTypeFiltersThrough() throws Exception {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(jobService.listJobsOffset(eq(JobStatus.FAILED), eq("send_email"), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/jobs").queryParam("status", "FAILED").queryParam("type", "send_email"))
                .andExpect(status().isOk());

        verify(jobService).listJobsOffset(eq(JobStatus.FAILED), eq("send_email"), any());
    }


    @Test
    void listJobs_withAfterParam_usesKeysetModeAndReturnsCursorEnvelope() throws Exception {
        Job job = new Job("resize_image", "{}", 0);
        when(jobService.listJobsKeyset(eq(null), eq(null), eq("some-cursor"), eq(20)))
                .thenReturn(new JobsKeysetPage(List.of(job), "next-cursor-value"));

        mockMvc.perform(get("/jobs").queryParam("after", "some-cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor-value"))
                .andExpect(jsonPath("$.totalElements").doesNotExist())
                .andExpect(jsonPath("$.page").doesNotExist());
    }

    @Test
    void listJobs_keysetMode_nullNextCursorWhenNoMorePages() throws Exception {
        when(jobService.listJobsKeyset(any(), any(), eq("cursor"), eq(20)))
                .thenReturn(new JobsKeysetPage(List.of(), null));

        mockMvc.perform(get("/jobs").queryParam("after", "cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void listJobs_keysetMode_passesStatusAndTypeFiltersThrough() throws Exception {
        when(jobService.listJobsKeyset(eq(JobStatus.FAILED), eq("resize_image"), eq("cursor"), eq(20)))
                .thenReturn(new JobsKeysetPage(List.of(), null));

        mockMvc.perform(get("/jobs")
                        .queryParam("after", "cursor")
                        .queryParam("status", "FAILED")
                        .queryParam("type", "resize_image"))
                .andExpect(status().isOk());

        verify(jobService).listJobsKeyset(JobStatus.FAILED, "resize_image", "cursor", 20);
    }

    @Test
    void adminJobsSummary_returnsWhateverServiceProvides() throws Exception {
        when(jobService.getAdminJobsSummary()).thenReturn(List.of(
                new com.example.job_queue_platform_refined.api.dto.AdminJobSummaryRow(null, JobStatus.FAILED, 3L)
        ));

        mockMvc.perform(get("/admin/jobs/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].count").value(3));
    }
}
