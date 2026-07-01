package com.jobqueue.queue;

import com.jobqueue.domain.Job;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week5")
class PriorityJobQueueTest {

    @Test
    void dequeuesInDescendingPriorityOrder() throws InterruptedException {
        PriorityJobQueue queue = new PriorityJobQueue();

        Job p1 = new Job("type", "{}", 1);
        Job p5 = new Job("type", "{}", 5);
        Job p3 = new Job("type", "{}", 3);
        Job p2 = new Job("type", "{}", 2);

        // Enqueue out of order, exactly as specced: 1, 5, 3, 2
        queue.enqueue(p1);
        queue.enqueue(p5);
        queue.enqueue(p3);
        queue.enqueue(p2);

        assertEquals(p5, queue.dequeue());
        assertEquals(p3, queue.dequeue());
        assertEquals(p2, queue.dequeue());
        assertEquals(p1, queue.dequeue());
    }

    @Test
    void sizeAndIsEmptyReflectQueueContents() {
        PriorityJobQueue queue = new PriorityJobQueue();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());

        queue.enqueue(new Job("type", "{}", 1));
        queue.enqueue(new Job("type", "{}", 2));

        assertFalse(queue.isEmpty());
        assertEquals(2, queue.size());
    }

    @Test
    void enqueueingNullThrows() {
        PriorityJobQueue queue = new PriorityJobQueue();
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
    }

    @Test
    void higherPriorityEnqueuedLastStillDequeuesFirst() throws InterruptedException {
        PriorityJobQueue queue = new PriorityJobQueue();

        Job low = new Job("type", "{}", 1);
        Job high = new Job("type", "{}", 10);

        queue.enqueue(low);
        queue.enqueue(high);

        // Even though "low" was enqueued first, "high" must come out first.
        assertEquals(high, queue.dequeue());
        assertEquals(low, queue.dequeue());
    }
}
