package com.example.job_queue_platform_refined.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String apiKey, int limit, Duration window) {
        String key = "rate:" + apiKey;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        Long currentCount = redisTemplate.opsForZSet().zCard(key);
        long count = (currentCount != null) ? currentCount : 0;

        if (count >= limit) {
            return false;
        }

        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
        redisTemplate.expire(key, window);

        return true;
    }

    public long getCurrentCount(String apiKey, Duration window) {
        String key = "rate:" + apiKey;
        long windowStart = System.currentTimeMillis() - window.toMillis();

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return (count != null) ? count : 0;
    }
}