package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.RefreshToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Tag("week9")
class RefreshTokenRepositoryCleanupTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void deleteStaleTokens_removesExpiredAndOldRevoked_keepsValidAndRecentlyRevoked() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant revokedCutoff = now.minus(Duration.ofDays(7));

        RefreshToken stillValid = new RefreshToken(userId, "hash-valid", now.plusSeconds(3600));
        RefreshToken genuinelyExpired = new RefreshToken(userId, "hash-expired", now.minusSeconds(10));

        RefreshToken revokedButRecent = new RefreshToken(userId, "hash-revoked-recent", now.plusSeconds(3600));
        revokedButRecent.setRevoked(true); 

        RefreshToken revokedLongAgo = new RefreshToken(userId, "hash-revoked-old", now.plusSeconds(3600));
        revokedLongAgo.setRevoked(true);

        refreshTokenRepository.save(stillValid);
        refreshTokenRepository.save(genuinelyExpired);
        refreshTokenRepository.save(revokedButRecent);
        RefreshToken savedOld = refreshTokenRepository.save(revokedLongAgo);

        backdateCreatedAt(savedOld.getId(), now.minus(Duration.ofDays(8)));

        int deletedCount = refreshTokenRepository.deleteStaleTokens(now, revokedCutoff);

        assertEquals(2, deletedCount, "expected exactly the expired row and the old-revoked row to be deleted");

        List<String> remainingHashes = refreshTokenRepository.findAll().stream()
                .map(RefreshToken::getTokenHash).toList();

        assertTrue(remainingHashes.contains("hash-valid"), "a still-valid token must survive cleanup");
        assertTrue(remainingHashes.contains("hash-revoked-recent"), "a recently-revoked token must survive within the retention window");
        assertFalse(remainingHashes.contains("hash-expired"), "a genuinely expired token must be deleted");
        assertFalse(remainingHashes.contains("hash-revoked-old"), "a token revoked past the retention window must be deleted");
    }


    private void backdateCreatedAt(UUID id, Instant createdAt) {
        RefreshToken token = refreshTokenRepository.findById(id).orElseThrow();
        try {
            var field = RefreshToken.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(token, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        refreshTokenRepository.save(token);
    }
}