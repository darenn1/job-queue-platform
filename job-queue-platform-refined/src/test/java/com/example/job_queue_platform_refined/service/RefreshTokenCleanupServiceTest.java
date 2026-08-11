package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week9")
class RefreshTokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void cleanupCallsDeleteWithNowAndTheConfiguredRetentionCutoff() {
        long retentionDays = 7;
        RefreshTokenCleanupService service = new RefreshTokenCleanupService(refreshTokenRepository, retentionDays);
        when(refreshTokenRepository.deleteStaleTokens(any(), any())).thenReturn(3);

        Instant before = Instant.now();
        service.cleanupStaleTokens();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteStaleTokens(nowCaptor.capture(), cutoffCaptor.capture());

        Instant capturedNow = nowCaptor.getValue();
        Instant capturedCutoff = cutoffCaptor.getValue();

        assertFalse(capturedNow.isBefore(before));
        assertFalse(capturedNow.isAfter(after));

        Duration actualGap = Duration.between(capturedCutoff, capturedNow);
        assertEquals(Duration.ofDays(retentionDays).toSeconds(), actualGap.toSeconds(),
                "cutoff should be exactly retentionDays before now");
    }

    @Test
    void cleanupDoesNotThrowWhenNothingWasDeleted() {
        RefreshTokenCleanupService service = new RefreshTokenCleanupService(refreshTokenRepository, 7);
        when(refreshTokenRepository.deleteStaleTokens(any(), any())).thenReturn(0);

        assertDoesNotThrow(service::cleanupStaleTokens);
    }

    @Test
    void differentRetentionConfigurationsProduceDifferentCutoffs() {
        RefreshTokenCleanupService shortRetention = new RefreshTokenCleanupService(refreshTokenRepository, 1);
        when(refreshTokenRepository.deleteStaleTokens(any(), any())).thenReturn(0);

        shortRetention.cleanupStaleTokens();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteStaleTokens(nowCaptor.capture(), cutoffCaptor.capture());

        Duration gap = Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue());
        assertEquals(Duration.ofDays(1).toSeconds(), gap.toSeconds());
    }
}