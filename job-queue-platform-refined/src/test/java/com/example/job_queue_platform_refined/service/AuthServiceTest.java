package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.api.dto.AuthResponse;
import com.example.job_queue_platform_refined.domain.RefreshToken;
import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.exception.DuplicateUserException;
import com.example.job_queue_platform_refined.exception.InvalidCredentialsException;
import com.example.job_queue_platform_refined.exception.InvalidRefreshTokenException;
import com.example.job_queue_platform_refined.repository.RefreshTokenRepository;
import com.example.job_queue_platform_refined.repository.UserRepository;
import com.example.job_queue_platform_refined.security.ApiKeyHasher;
import com.example.job_queue_platform_refined.security.JwtService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week9")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private static final long REFRESH_TTL_MS = 2_592_000_000L; // 30 days

    private AuthService newService() {
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, REFRESH_TTL_MS);
    }

    @Test
    void registerReturnsBothAccessAndRefreshTokens() {
        AuthService authService = newService();
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("bcrypt-hash");
        when(jwtService.generateToken("alice", "USER")).thenReturn("fake-access-token");

        AuthResponse response = authService.register("alice", "alice@example.com", "plaintext-password");

        assertEquals("fake-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(ApiKeyHasher.hash(response.getRefreshToken()), captor.getValue().getTokenHash());
        assertFalse(captor.getValue().isRevoked());
    }

    @Test
    void registerThrowsForExistingUsername() {
        AuthService authService = newService();
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register("alice", "x@y.com", "password123"));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void loginReturnsBothTokensOnSuccess() {
        AuthService authService = newService();
        User user = new User("alice", "alice@example.com", "bcrypt-hash", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken("alice", "USER")).thenReturn("fake-access-token");

        AuthResponse response = authService.login("alice", "correct-password");

        assertEquals("fake-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void loginThrowsInvalidCredentialsForWrongPassword() {
        AuthService authService = newService();
        User user = new User("alice", "alice@example.com", "bcrypt-hash", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login("alice", "wrong"));
    }

    @Test
    void refreshWithValidTokenRotatesIt_revokesOldIssuesNew() {
        AuthService authService = newService();
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "raw-refresh-token-value";
        RefreshToken existing = new RefreshToken(userId, ApiKeyHasher.hash(rawRefreshToken), Instant.now().plusSeconds(3600));
        User user = new User("alice", "alice@example.com", "hash", Role.USER);

        when(refreshTokenRepository.findByTokenHash(ApiKeyHasher.hash(rawRefreshToken))).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice", "USER")).thenReturn("new-access-token");

        AuthResponse response = authService.refreshAccessToken(rawRefreshToken);

        assertEquals("new-access-token", response.getAccessToken());
        assertNotEquals(rawRefreshToken, response.getRefreshToken(),
                "rotation must issue a DIFFERENT refresh token, not reuse the old one");

        assertTrue(existing.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // revoke old + save new
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void refreshWithExpiredTokenThrowsWithoutRevokingEverything() {
        AuthService authService = newService();
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "expired-token";
        RefreshToken existing = new RefreshToken(userId, ApiKeyHasher.hash(rawRefreshToken), Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(ApiKeyHasher.hash(rawRefreshToken))).thenReturn(Optional.of(existing));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshAccessToken(rawRefreshToken));

        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void refreshWithUnknownTokenThrows() {
        AuthService authService = newService();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshAccessToken("never-issued-token"));
    }

    @Test
    void refreshWithAnAlreadyRevokedToken_triggersReuseDetection_revokesEverythingForThatUser() {
        AuthService authService = newService();
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "stolen-and-reused-token";
        RefreshToken alreadyRevoked = new RefreshToken(userId, ApiKeyHasher.hash(rawRefreshToken), Instant.now().plusSeconds(3600));
        alreadyRevoked.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(ApiKeyHasher.hash(rawRefreshToken))).thenReturn(Optional.of(alreadyRevoked));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshAccessToken(rawRefreshToken));

        verify(refreshTokenRepository).revokeAllForUser(userId);
        verifyNoInteractions(jwtService);
    }

    @Test
    void logoutRevokesOnlyThePresentedToken() {
        AuthService authService = newService();
        String rawRefreshToken = "token-to-logout";
        RefreshToken token = new RefreshToken(UUID.randomUUID(), ApiKeyHasher.hash(rawRefreshToken), Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(ApiKeyHasher.hash(rawRefreshToken))).thenReturn(Optional.of(token));

        authService.logout(rawRefreshToken);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void logoutOnAnUnknownTokenDoesNotThrow_idempotent() {
        AuthService authService = newService();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.logout("never-issued-token"));
    }
}