package com.example.job_queue_platform_refined.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("week10")
class RedisConnectivityTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void setThenGetReturnsTheSameValue() {
        redisTemplate.opsForValue().set("test:hello", "world");

        assertEquals("world", redisTemplate.opsForValue().get("test:hello"));

        redisTemplate.delete("test:hello");
    }

    @Test
    void keyWithTtlExpiresAfterTheGivenDuration() throws InterruptedException {
        redisTemplate.opsForValue().set("test:expiring", "temporary", Expiration.seconds(1));

        assertEquals("temporary", redisTemplate.opsForValue().get("test:expiring"));

        long deadline = System.currentTimeMillis() + 3000;
        String value = redisTemplate.opsForValue().get("test:expiring");
        while (value != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            value = redisTemplate.opsForValue().get("test:expiring");
        }

        assertNull(value, "expected key to have expired via its TTL");
    }
}