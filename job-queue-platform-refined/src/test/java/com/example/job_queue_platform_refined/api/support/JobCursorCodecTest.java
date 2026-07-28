package com.example.job_queue_platform_refined.api.support;

import com.example.job_queue_platform_refined.domain.JobStatus;
import com.example.job_queue_platform_refined.exception.InvalidCursorException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week8_5")
class JobCursorCodecTest {

    @Test
    void encodeThenDecodeRoundTripsExactly() {
        JobCursor original = new JobCursor(Instant.now(), UUID.randomUUID(), JobStatus.FAILED, "send_email");

        String encoded = JobCursorCodec.encode(original);
        JobCursor decoded = JobCursorCodec.decode(encoded);

        assertEquals(original.lastCreatedAt().toEpochMilli(), decoded.lastCreatedAt().toEpochMilli());
        assertEquals(original.lastId(), decoded.lastId());
        assertEquals(original.status(), decoded.status());
        assertEquals(original.type(), decoded.type());
    }

    @Test
    void encodeThenDecodeRoundTripsWithNullFilters() {
        JobCursor original = new JobCursor(Instant.now(), UUID.randomUUID(), null, null);

        JobCursor decoded = JobCursorCodec.decode(JobCursorCodec.encode(original));

        assertNull(decoded.status());
        assertNull(decoded.type());
    }

    @Test
    void decodeThrowsInvalidCursorExceptionForGarbageInput() {
        assertThrows(InvalidCursorException.class, () -> JobCursorCodec.decode("not-a-real-cursor!!"));
    }

    @Test
    void decodeThrowsInvalidCursorExceptionForTamperedButValidBase64() {
        String tampered = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("wrong|format|entirely".getBytes());

        assertThrows(InvalidCursorException.class, () -> JobCursorCodec.decode(tampered));
    }
}