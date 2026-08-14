package com.example.job_queue_platform_refined.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("week10")
class RedisJobQueueTest {

    @Autowired
    private RedisJobQueue redisJobQueue;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(RedisJobQueue.QUEUE_KEY);
        redisTemplate.delete(RedisJobQueue.DEAD_LETTER_KEY);
    }

    @Test
    void enqueuedItemsAreDequeuedInFifoOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();

        redisJobQueue.enqueue(first);
        redisJobQueue.enqueue(second);
        redisJobQueue.enqueue(third);

        assertEquals(Optional.of(first), redisJobQueue.dequeue(Duration.ofSeconds(1)));
        assertEquals(Optional.of(second), redisJobQueue.dequeue(Duration.ofSeconds(1)));
        assertEquals(Optional.of(third), redisJobQueue.dequeue(Duration.ofSeconds(1)));
    }

    @Test
    void dequeueReturnsEmptyAfterTimeoutWhenQueueIsEmpty() {
        Optional<UUID> result = redisJobQueue.dequeue(Duration.ofMillis(500));

        assertTrue(result.isEmpty());
    }

    @Test
    void enqueuedItemsSurviveWithNoConsumerDrainingThem_theDurabilityProperty() {
        List<UUID> submitted = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            submitted.add(id);
            redisJobQueue.enqueue(id);
        }

        assertEquals(5, redisJobQueue.size(), "all 5 items should still be sitting in the queue, untouched");

        for (UUID expected : submitted) {
            assertEquals(Optional.of(expected), redisJobQueue.dequeue(Duration.ofSeconds(1)));
        }
        assertEquals(0, redisJobQueue.size());
    }

    @Test
    void deadLetterQueueIsSeparateFromTheMainQueue() {
        UUID mainQueueJob = UUID.randomUUID();
        UUID deadLetterJob = UUID.randomUUID();

        redisJobQueue.enqueue(mainQueueJob);
        redisJobQueue.enqueueDeadLetter(deadLetterJob);

        assertEquals(1, redisJobQueue.size());
        assertEquals(1, redisJobQueue.deadLetterSize());
        assertEquals(Optional.of(mainQueueJob), redisJobQueue.dequeue(Duration.ofSeconds(1)));
    }
}