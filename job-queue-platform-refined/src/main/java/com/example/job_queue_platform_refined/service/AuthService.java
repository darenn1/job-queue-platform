package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.api.dto.AuthResponse;
import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.exception.DuplicateUserException;
import com.example.job_queue_platform_refined.exception.InvalidCredentialsException;
import com.example.job_queue_platform_refined.repository.UserRepository;
import com.example.job_queue_platform_refined.security.ApiKeyHasher;
import com.example.job_queue_platform_refined.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public String generateApiKey(String username) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        user.setApiKeyHash(ApiKeyHasher.hash(apiKey));
        userRepository.save(user);

        return apiKey;
    }
}