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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtl = Duration.ofMillis(refreshExpirationMs);
    }

    public AuthResponse register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("Email already registered: " + email);
        }

        String hash = passwordEncoder.encode(rawPassword);
        User user = new User(username, email, hash, Role.USER);
        userRepository.save(user);

        return issueTokenPair(user);
    }

    public AuthResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokenPair(user);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        String hash = ApiKeyHasher.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(existing.getUserId());
            throw new InvalidRefreshTokenException();
        }

        if (existing.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = ApiKeyHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateToken(user.getUsername(), user.getRole().name());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                ApiKeyHasher.hash(rawRefreshToken),
                Instant.now().plus(refreshTokenTtl)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, user.getUsername(), user.getRole().name());
    }

    public String generateApiKey(String username) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawApiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        user.setApiKeyHash(ApiKeyHasher.hash(rawApiKey));
        userRepository.save(user);

        return rawApiKey;
    }
}
