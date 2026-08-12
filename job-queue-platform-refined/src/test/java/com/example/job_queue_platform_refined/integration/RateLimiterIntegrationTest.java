package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.security.RateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("week10")
class RateLimiterIntegrationTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private String apiKey;

    @AfterEach
    void cleanUp() {
        if (apiKey != null) {
            redisTemplate.delete("rate:" + apiKey);
        }
    }

    @Test
    void allowsExactlyTheLimitAndRejectsTheNextOne() {
        apiKey = "test-" + UUID.randomUUID();
        int limit = 5;

        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiter.isAllowed(apiKey, limit, Duration.ofSeconds(60)),
                    "request " + (i + 1) + " of " + limit + " should be allowed");
        }

        assertFalse(rateLimiter.isAllowed(apiKey, limit, Duration.ofSeconds(60)),
                "the (limit + 1)th request should be rejected");
    }

    @Test
    void slidingWindowPreventsDoubleBurstAcrossWhatWouldBeAFixedWindowBoundary() throws InterruptedException {
        apiKey = "test-" + UUID.randomUUID();
        int limit = 5;
        Duration window = Duration.ofSeconds(2); 

        for (int i = 0; i < limit; i++) {
            assertTrue(rateLimiter.isAllowed(apiKey, limit, window));
        }
        assertFalse(rateLimiter.isAllowed(apiKey, limit, window),
                "should be rejected immediately after exhausting the limit");

        Thread.sleep(500);
        assertFalse(rateLimiter.isAllowed(apiKey, limit, window),
                "should still be rejected mid-window — nothing has aged out yet");

        Thread.sleep(1700); 
        assertTrue(rateLimiter.isAllowed(apiKey, limit, window),
                "should be allowed again once the original burst has fully aged out of the window");
    }
}