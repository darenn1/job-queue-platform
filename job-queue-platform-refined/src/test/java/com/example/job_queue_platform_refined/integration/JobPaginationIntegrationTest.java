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
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("week8_5")
class JobPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JobRepository jobRepository;

    private static final String[] TYPES = {"send_email", "resize_image", "generate_report"};

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Test
    void offsetFirstPageForPersistedJobsIncludesNextCursor() throws Exception {
        for (int i = 0; i < 3; i++) {
            jobRepository.save(new Job("send_email", "{}", i));
        }

        String body = mockMvc.perform(get("/jobs").queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isString())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> parsed = jsonMapper.readValue(body, Map.class);
        String cursor = (String) parsed.get("nextCursor");
        assertNotNull(cursor, "persisted jobs should include enough data to build a cursor");
        assertFalse(cursor.isBlank(), "nextCursor should not be blank when another page exists");
    }

    @Test
    void paginatingByStatusFilterCollectsExactlyAsManyJobsAsExistInTheDb() throws Exception {
        List<UUID> submittedIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String type = TYPES[i % TYPES.length];
            Map<String, Object> request = Map.of(
                    "type", type,
                    "payload", "{}",
                    "priority", i % 5
            );

            String responseBody = mockMvc.perform(post("/jobs")
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
        assertTrue(actualFailedCountInDb >= forcedFailedCount,
                "DB should have at least the forced FAILED jobs (plus any that failed naturally)");

        Set<UUID> collectedIds = new HashSet<>();
        String cursor = null;
        int pagesFetched = 0;
        int maxPages = 100; 

        do {
            var requestBuilder = get("/jobs")
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
                boolean isNew = collectedIds.add(UUID.fromString(idString));
                assertTrue(isNew, "job " + idString + " appeared on more than one page — duplicate");
            }

            cursor = (String) parsed.get("nextCursor");
            pagesFetched++;
            assertTrue(pagesFetched <= maxPages, "exceeded max pages — likely an infinite pagination loop");

        } while (cursor != null);

        assertEquals(actualFailedCountInDb, collectedIds.size(),
                "total jobs collected across all paginated pages should exactly match countByStatus(FAILED) in the DB");
        assertTrue(pagesFetched > 1, "expected pagination across multiple pages given size=5 and " + actualFailedCountInDb + " FAILED jobs");
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
