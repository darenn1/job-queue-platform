package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class RefreshTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration revokedRetention;

    public RefreshTokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                                       @Value("${refresh-token.revoked-retention-days:7}") long revokedRetentionDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedRetention = Duration.ofDays(revokedRetentionDays);
    }

    @Scheduled(cron = "${refresh-token.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupStaleTokens() {
        Instant now = Instant.now();
        Instant revokedCutoff = now.minus(revokedRetention);

        int deleted = refreshTokenRepository.deleteStaleTokens(now, revokedCutoff);

        if (deleted > 0) {
            log.info("Refresh token cleanup: removed {} stale row(s).", deleted);
        }
    }
}