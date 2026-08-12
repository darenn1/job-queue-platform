package com.example.job_queue_platform_refined.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week10")
class RateLimiterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Test
    void allowsRequestWhenUnderTheLimit() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard("rate:test-key")).thenReturn(5L);

        RateLimiter rateLimiter = new RateLimiter(redisTemplate);
        boolean allowed = rateLimiter.isAllowed("test-key", 10, Duration.ofSeconds(60));

        assertTrue(allowed);
        verify(zSetOperations).add(eq("rate:test-key"), anyString(), anyDouble());
        verify(redisTemplate).expire(eq("rate:test-key"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void rejectsRequestWhenAtTheLimit() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard("rate:test-key")).thenReturn(10L); // already exactly at the limit

        RateLimiter rateLimiter = new RateLimiter(redisTemplate);
        boolean allowed = rateLimiter.isAllowed("test-key", 10, Duration.ofSeconds(60));

        assertFalse(allowed);
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void alwaysPrunesStaleEntriesBeforeCounting() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard("rate:test-key")).thenReturn(0L);

        RateLimiter rateLimiter = new RateLimiter(redisTemplate);
        rateLimiter.isAllowed("test-key", 10, Duration.ofSeconds(60));

        verify(zSetOperations).removeRangeByScore(eq("rate:test-key"), eq(0.0), anyDouble());
    }

    @Test
    void getCurrentCountReturnsZeroForANonexistentKey() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard("rate:brand-new-key")).thenReturn(null); // Redis returns null, not 0, for a key that's never existed

        RateLimiter rateLimiter = new RateLimiter(redisTemplate);
        long count = rateLimiter.getCurrentCount("brand-new-key", Duration.ofSeconds(60));

        assertEquals(0, count);
    }
}