package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.SubmitJobRequest;
import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
@AutoConfigureMockMvc
@Tag("week9")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private JobService jobService;

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        User user = new User("test-user", "test@example.com", "hash", Role.USER);
        setId(user, CURRENT_USER_ID);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(auth));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        User user = new User("test-admin", "admin@example.com", "hash", Role.ADMIN);
        setId(user, CURRENT_USER_ID);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(auth));
    }

    private static void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void submitJob_persistsAndReturns201() throws Exception {
        SubmitJobRequest request = new SubmitJobRequest("send_email", "{\"to\":\"a@b.com\"}", 3);
        Job saved = new Job("send_email", "{\"to\":\"a@b.com\"}", 3);
        saved.setSubmittedBy(CURRENT_USER_ID);
        when(jobService.submitJob("send_email", "{\"to\":\"a@b.com\"}", 3, CURRENT_USER_ID)).thenReturn(saved);

        mockMvc.perform(asUser(post("/jobs"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("send_email"));
    }

    @Test
    void getJob_returns200WhenFoundAndOwnedByCaller() throws Exception {
        UUID id = UUID.randomUUID();
        Job job = new Job("resize_image", "{}", 0);
        when(jobService.getJob(id, CURRENT_USER_ID)).thenReturn(job);

        mockMvc.perform(asUser(get("/jobs/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("resize_image"));
    }

    @Test
    void getJob_returns404_whenServiceThrowsNotFound_egSomeoneElsesJob() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getJob(id, CURRENT_USER_ID)).thenThrow(new JobNotFoundException(id));

        mockMvc.perform(asUser(get("/jobs/{id}", id)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listJobs_offsetMode_scopesToCurrentUser() throws Exception {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(jobService.listJobsOffset(eq(CURRENT_USER_ID), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(asUser(get("/jobs")))
                .andExpect(status().isOk());

        verify(jobService).listJobsOffset(eq(CURRENT_USER_ID), eq(null), eq(null), any());
    }

    @Test
    void listJobs_keysetMode_scopesToCurrentUser() throws Exception {
        when(jobService.listJobsKeyset(eq(CURRENT_USER_ID), eq(JobStatus.FAILED), eq(null), eq("cursor"), eq(20)))
                .thenReturn(new JobsKeysetPage(List.of(), null));

        mockMvc.perform(asUser(get("/jobs"))
                        .queryParam("after", "cursor")
                        .queryParam("status", "FAILED"))
                .andExpect(status().isOk());

        verify(jobService).listJobsKeyset(CURRENT_USER_ID, JobStatus.FAILED, null, "cursor", 20);
    }

    @Test
    void adminJobsSummary_returnsWhateverServiceProvides() throws Exception {
        when(jobService.getAdminJobsSummary()).thenReturn(List.of(
                new com.example.job_queue_platform_refined.api.dto.AdminJobSummaryRow(null, JobStatus.FAILED, 3L)
        ));

        mockMvc.perform(asAdmin(get("/admin/jobs/summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"));
    }
}
