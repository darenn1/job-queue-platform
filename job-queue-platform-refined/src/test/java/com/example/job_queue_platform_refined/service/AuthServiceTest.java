package com.example.job_queue_platform_refined.service;

import com.example.job_queue_platform_refined.api.dto.AuthResponse;
import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.exception.DuplicateUserException;
import com.example.job_queue_platform_refined.exception.InvalidCredentialsException;
import com.example.job_queue_platform_refined.repository.UserRepository;
import com.example.job_queue_platform_refined.security.JwtService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week9")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @Test
    void registerHashesThePasswordAndReturnsAToken() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("bcrypt-hash");
        when(jwtService.generateToken("alice", "USER")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register("alice", "alice@example.com", "plaintext-password");

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("alice", response.getUsername());
        assertEquals("USER", response.getRole());

        // The critical assertion: the RAW password never reaches save() — only the hash does.
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals("bcrypt-hash")));
        verify(passwordEncoder).encode("plaintext-password");
    }

    @Test
    void registerThrowsDuplicateUserExceptionForExistingUsername() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(DuplicateUserException.class,
                () -> authService.register("alice", "new@example.com", "password123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerThrowsDuplicateUserExceptionForExistingEmail() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class,
                () -> authService.register("newuser", "taken@example.com", "password123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = new User("alice", "alice@example.com", "bcrypt-hash", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateToken("alice", "USER")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login("alice", "correct-password");

        assertEquals("fake-jwt-token", response.getToken());
    }

    @Test
    void loginThrowsInvalidCredentialsForWrongPassword() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        User user = new User("alice", "alice@example.com", "bcrypt-hash", Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login("alice", "wrong-password"));
    }

    @Test
    void loginThrowsInvalidCredentialsForUnknownUsername_sameExceptionAsWrongPassword() {
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login("ghost", "anything"));
    }
}