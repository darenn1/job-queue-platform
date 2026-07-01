package com.jobqueue.queue;

import com.jobqueue.domain.Job;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week5")
class InMemoryJobQueueTest {

    @Test
    void enqueueTenJobsThenSizeIsTen() {
        InMemoryJobQueue queue = new InMemoryJobQueue();

        for (int i = 0; i < 10; i++) {
            queue.enqueue(new Job("send_email", "{}", 1));
        }

        assertEquals(10, queue.size());
        assertFalse(queue.isEmpty());
    }

    @Test
    void dequeueAllThenQueueIsEmpty() throws InterruptedException {
        InMemoryJobQueue queue = new InMemoryJobQueue();
        for (int i = 0; i < 10; i++) {
            queue.enqueue(new Job("send_email", "{}", 1));
        }

        for (int i = 0; i < 10; i++) {
            assertNotNull(queue.dequeue());
        }

        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void dequeueReturnsJobsInFifoOrder() throws InterruptedException {
        InMemoryJobQueue queue = new InMemoryJobQueue();
        Job first = new Job("send_email", "{}", 1);
        Job second = new Job("resize_image", "{}", 1);
        Job third = new Job("generate_report", "{}", 1);

        queue.enqueue(first);
        queue.enqueue(second);
        queue.enqueue(third);

        assertEquals(first, queue.dequeue());
        assertEquals(second, queue.dequeue());
        assertEquals(third, queue.dequeue());
    }

    @Test
    void enqueueingNullThrows() {
        InMemoryJobQueue queue = new InMemoryJobQueue();
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
    }

    @Test
    void dequeueBlocksUntilJobIsAvailable() throws Exception {
        InMemoryJobQueue queue = new InMemoryJobQueue();
        Job jobToDeliver = new Job("send_email", "{}", 1);

        Thread consumer = new Thread(() -> {
            try {
                Job received = queue.dequeue();
                assertEquals(jobToDeliver, received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        // Give the consumer a moment to start blocking on take().
        Thread.sleep(50);
        queue.enqueue(jobToDeliver);

        consumer.join(1000);
        assertFalse(consumer.isAlive(), "Consumer thread should have received the job and finished");
    }
}
