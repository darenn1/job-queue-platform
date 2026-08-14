package com.example.job_queue_platform_refined.worker;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisJobQueue {

    static final String QUEUE_KEY = "jobs:queue";
    static final String DEAD_LETTER_KEY = "jobs:deadletter";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisJobQueue(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void enqueue(UUID jobId) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, jobId.toString());
    }

    public Optional<UUID> dequeue(Duration timeout) {
        String value = redisTemplate.opsForList().rightPop(QUEUE_KEY, timeout);
        return (value != null) ? Optional.of(UUID.fromString(value)) : Optional.empty();
    }

    public long size() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return (size != null) ? size : 0;
    }

    public void enqueueDeadLetter(UUID jobId) {
        redisTemplate.opsForList().leftPush(DEAD_LETTER_KEY, jobId.toString());
    }

    public long deadLetterSize() {
        Long size = redisTemplate.opsForList().size(DEAD_LETTER_KEY);
        return (size != null) ? size : 0;
    }
}