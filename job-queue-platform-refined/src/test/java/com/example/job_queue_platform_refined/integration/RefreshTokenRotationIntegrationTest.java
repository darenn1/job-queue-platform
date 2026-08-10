package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.repository.RefreshTokenRepository;
import com.example.job_queue_platform_refined.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("week9")
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Map<?, ?> register() throws Exception {
        String username = "rotation-user-" + UUID.randomUUID();
        String body = mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "username", username, "email", username + "@example.com", "password", "password123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readValue(body, Map.class);
    }

    @Test
    void refreshingRotatesTheToken_oldTokenNoLongerWorksAfterward() throws Exception {
        Map<?, ?> registerResponse = register();
        String originalRefreshToken = (String) registerResponse.get("refreshToken");

        String refreshBody = mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", originalRefreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> refreshResponse = jsonMapper.readValue(refreshBody, Map.class);
        String newRefreshToken = (String) refreshResponse.get("refreshToken");

        assertNotEquals(originalRefreshToken, newRefreshToken, "rotation must issue a new refresh token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", originalRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newRefreshTokenAfterRotationWorksNormally() throws Exception {
        Map<?, ?> registerResponse = register();
        String originalRefreshToken = (String) registerResponse.get("refreshToken");

        String refreshBody = mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", originalRefreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRefreshToken = (String) jsonMapper.readValue(refreshBody, Map.class).get("refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", newRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    void reusingARotatedAwayTokenAfterANewOneWasAlreadyIssued_alsoInvalidatesTheNewOne() throws Exception {
        Map<?, ?> registerResponse = register();
        String originalRefreshToken = (String) registerResponse.get("refreshToken");

        String refreshBody = mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", originalRefreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondRefreshToken = (String) jsonMapper.readValue(refreshBody, Map.class).get("refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", originalRefreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", secondRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutThenRefreshWithTheSameTokenFails() throws Exception {
        Map<?, ?> registerResponse = register();
        String refreshToken = (String) registerResponse.get("refreshToken");

        mockMvc.perform(post("/auth/logout")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }
}