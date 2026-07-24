package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("week7")
class JobLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JobRepository jobRepository;

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Test
    @Transactional
    void submitJob_persistsWithPendingStatus() throws Exception {
        Map<String, Object> request = Map.of(
                "type", "send_email",
                "payload", "{\"to\":\"a@b.com\"}",
                "priority", 1
        );

        mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitJob_isPickedUpByWorkerAndReachesATerminalStatus() throws Exception {
        Map<String, Object> request = Map.of(
                "type", "send_email",
                "payload", "{\"to\":\"a@b.com\"}",
                "priority", 1
        );

        String responseBody = mockMvc.perform(post("/jobs")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID jobId = UUID.fromString(
                (String) jsonMapper.readValue(responseBody, Map.class).get("id"));

        JobStatus finalStatus = pollUntilTerminal(jobId, 3000);

        assertTrue(finalStatus == JobStatus.COMPLETED || finalStatus == JobStatus.FAILED,
                "expected the worker to reach a terminal status within the timeout, was: " + finalStatus);
    }

    private JobStatus pollUntilTerminal(UUID jobId, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Optional<Job> job = jobRepository.findById(jobId);
            if (job.isPresent()) {
                JobStatus status = job.get().getStatus();
                if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                    return status;
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Job " + jobId + " did not reach a terminal status within " + timeoutMillis + "ms");
    }
}