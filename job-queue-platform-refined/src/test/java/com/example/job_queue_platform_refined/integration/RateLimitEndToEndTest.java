package com.example.job_queue_platform_refined.integration;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"rate-limit.requests-per-window=100", "rate-limit.window-seconds=60"})
@AutoConfigureMockMvc
@Tag("week10")
class RateLimitEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void the101stRequestInAWindowReturns429WithRateLimitHeaders() throws Exception {
        String uniqueKey = "e2e-test-" + UUID.randomUUID();
        String loginBody = jsonMapper.writeValueAsString(Map.of("username", "nobody", "password", "wrong"));

        for (int i = 0; i < 100; i++) {
            int status = mockMvc.perform(post("/auth/login")
                            .header("X-API-Key", uniqueKey)
                            .contentType("application/json")
                            .content(loginBody))
                    .andReturn().getResponse().getStatus();

            assertNotEquals(429, status, "request " + (i + 1) + " of 100 should not be rate-limited yet");
        }

        mockMvc.perform(post("/auth/login")
                        .header("X-API-Key", uniqueKey)
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(result -> {
                    assertEquals(429, result.getResponse().getStatus());
                    assertEquals("100", result.getResponse().getHeader("X-RateLimit-Limit"));
                    assertEquals("0", result.getResponse().getHeader("X-RateLimit-Remaining"));
                    assertEquals("60", result.getResponse().getHeader("Retry-After"));
                });
    }
}