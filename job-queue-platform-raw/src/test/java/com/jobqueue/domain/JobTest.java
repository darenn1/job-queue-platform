package com.jobqueue.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week5")
class JobTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void convenienceConstructorGeneratesIdAndDefaultsToPending() {
        Job job = new Job("send_email", "{\"to\":\"a@example.com\"}", 5);

        assertNotNull(job.getId());
        assertEquals("send_email", job.getType());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(5, job.getPriority());
        assertNotNull(job.getCreatedAt());
        assertEquals(job.getCreatedAt(), job.getUpdatedAt());
    }

    @Test
    void settingStatusUpdatesTheTimestamp() throws InterruptedException {
        Job job = new Job("resize_image", "{}", 1);
        var originalUpdatedAt = job.getUpdatedAt();

        Thread.sleep(5); // ensure clock moves forward
        job.setStatus(JobStatus.RUNNING);

        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertTrue(job.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void getterAndSetterRoundTripForAllFields() {
        Job job = new Job();
        UUID id = UUID.randomUUID();

        job.setId(id);
        job.setType("generate_report");
        job.setPayload("{\"reportId\":42}");
        job.setStatus(JobStatus.COMPLETED);
        job.setPriority(3);
        job.setResult("ok");

        assertEquals(id, job.getId());
        assertEquals("generate_report", job.getType());
        assertEquals("{\"reportId\":42}", job.getPayload());
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(3, job.getPriority());
        assertEquals("ok", job.getResult());
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        UUID sharedId = UUID.randomUUID();
        Job a = new Job(sharedId, "send_email", "{}", JobStatus.PENDING, 1, null, null, null);
        Job b = new Job(sharedId, "resize_image", "{}", JobStatus.RUNNING, 9, null, null, null);

        assertEquals(a, b, "Jobs with the same id should be equal regardless of other fields");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void serializesToJsonAndBackPreservingFields() throws Exception {
        Job original = new Job("send_email", "{\"to\":\"a@example.com\"}", 7);

        String json = objectMapper.writeValueAsString(original);
        Job deserialized = objectMapper.readValue(json, Job.class);

        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getPayload(), deserialized.getPayload());
        assertEquals(original.getStatus(), deserialized.getStatus());
        assertEquals(original.getPriority(), deserialized.getPriority());
    }
}
