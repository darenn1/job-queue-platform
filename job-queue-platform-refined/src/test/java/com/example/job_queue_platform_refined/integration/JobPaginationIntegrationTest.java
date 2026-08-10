package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.repository.JobRepository;
import com.example.job_queue_platform_refined.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("week9")
class JobPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String[] TYPES = {"send_email", "resize_image", "generate_report"};

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndGetToken() throws Exception {
        String username = "page-" + UUID.randomUUID();
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
    void paginatingByStatusFilterCollectsExactlyAsManyJobsAsExistInTheDb() throws Exception {
        String token = registerAndGetToken();

        List<UUID> submittedIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String type = TYPES[i % TYPES.length];
            Map<String, Object> request = Map.of("type", type, "payload", "{}", "priority", i % 5);

            String responseBody = mockMvc.perform(post("/jobs")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID id = UUID.fromString((String) jsonMapper.readValue(responseBody, Map.class).get("id"));
            submittedIds.add(id);
        }

        waitUntilAllTerminal(submittedIds, 10_000);

        int forcedFailedCount = 0;
        for (int i = 0; i < submittedIds.size(); i++) {
            if (i % 4 == 0) {
                Job job = jobRepository.findById(submittedIds.get(i)).orElseThrow();
                job.setStatus(JobStatus.FAILED);
                jobRepository.save(job);
                forcedFailedCount++;
            }
        }

        long actualFailedCountInDb = jobRepository.countByStatus(JobStatus.FAILED);
        assertTrue(actualFailedCountInDb >= forcedFailedCount);

        Set<UUID> collectedIds = new HashSet<>();
        String cursor = null;
        int pagesFetched = 0;
        int maxPages = 100;

        do {
            MockHttpServletRequestBuilder requestBuilder = get("/jobs")
                    .header("Authorization", "Bearer " + token)
                    .queryParam("status", "FAILED")
                    .queryParam("size", "5");
            if (cursor != null) {
                requestBuilder = requestBuilder.queryParam("after", cursor);
            }

            String body = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Map<?, ?> parsed = jsonMapper.readValue(body, Map.class);
            List<?> content = (List<?>) parsed.get("content");
            for (Object item : content) {
                String idString = (String) ((Map<?, ?>) item).get("id");
                assertTrue(collectedIds.add(UUID.fromString(idString)),
                        "job " + idString + " appeared on more than one page — duplicate");
            }

            cursor = (String) parsed.get("nextCursor");
            pagesFetched++;
            assertTrue(pagesFetched <= maxPages, "exceeded max pages — likely an infinite pagination loop");

        } while (cursor != null);

        assertEquals(actualFailedCountInDb, collectedIds.size(),
                "total jobs collected across all paginated pages should exactly match countByStatus(FAILED) in the DB");
        assertTrue(pagesFetched > 1,
                "expected pagination across multiple pages given size=5 and " + actualFailedCountInDb + " FAILED jobs");
    }

    private void waitUntilAllTerminal(List<UUID> ids, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            long stillInFlight = ids.stream()
                    .map(id -> jobRepository.findById(id).orElseThrow().getStatus())
                    .filter(status -> status == JobStatus.PENDING || status == JobStatus.RUNNING)
                    .count();
            if (stillInFlight == 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Not all submitted jobs reached a terminal status within " + timeoutMillis + "ms");
    }
}
