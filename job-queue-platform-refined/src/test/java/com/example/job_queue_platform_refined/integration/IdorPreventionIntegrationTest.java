package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("week9")
class IdorPreventionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndGetToken(String username) throws Exception {
        Map<String, Object> registerRequest = Map.of(
                "username", username, "email", username + "@example.com", "password", "password123");

        String body = mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return (String) jsonMapper.readValue(body, Map.class).get("accessToken");
    }

    @Test
    void userCannotFetchAnotherUsersJobByGuessingItsId() throws Exception {
        String userAToken = registerAndGetToken("user-a-" + UUID.randomUUID());
        String userBToken = registerAndGetToken("user-b-" + UUID.randomUUID());

        Map<String, Object> request = Map.of("type", "send_email", "payload", "{}", "priority", 0);
        String responseBody = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String jobId = (String) jsonMapper.readValue(responseBody, Map.class).get("id");

        mockMvc.perform(get("/jobs/{id}", jobId).header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs/{id}", jobId).header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound()); 
    }

    @Test
    void userCannotSeeAnotherUsersJobInTheirOwnJobsList() throws Exception {
        String userAToken = registerAndGetToken("user-a-" + UUID.randomUUID());
        String userBToken = registerAndGetToken("user-b-" + UUID.randomUUID());

        Map<String, Object> request = Map.of("type", "resize_image", "payload", "{}", "priority", 0);
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String userBJobs = mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> parsed = jsonMapper.readValue(userBJobs, Map.class);
        List<?> content = (List<?>) parsed.get("content");

        assertTrue(content.isEmpty(), "User B's job list must not contain User A's job — got: " + content);
    }
}