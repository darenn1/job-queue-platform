package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR (r.revoked = true AND r.createdAt < :revokedCutoff)")
    int deleteStaleTokens(@Param("now") Instant now, @Param("revokedCutoff") Instant revokedCutoff);
}
