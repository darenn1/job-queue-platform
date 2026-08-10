package com.example.job_queue_platform_refined.api;

import com.example.job_queue_platform_refined.api.dto.AuthResponse;
import com.example.job_queue_platform_refined.exception.DuplicateUserException;
import com.example.job_queue_platform_refined.exception.InvalidCredentialsException;
import com.example.job_queue_platform_refined.exception.InvalidRefreshTokenException;
import com.example.job_queue_platform_refined.security.JwtAuthenticationFilter;
import com.example.job_queue_platform_refined.service.AuthService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("week9")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_returns201WithBothTokens() throws Exception {
        when(authService.register("alice", "alice@example.com", "password123"))
                .thenReturn(new AuthResponse("fake-access", "fake-refresh", "alice", "USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(
                                Map.of("username", "alice", "email", "alice@example.com", "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("fake-access"))
                .andExpect(jsonPath("$.refreshToken").value("fake-refresh"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(
                                Map.of("username", "alice", "email", "alice@example.com", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("password"));
    }

    @Test
    void register_returns409ForDuplicateUsername() throws Exception {
        when(authService.register("alice", "alice@example.com", "password123"))
                .thenThrow(new DuplicateUserException("Username already taken: alice"));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(
                                Map.of("username", "alice", "email", "alice@example.com", "password", "password123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200WithBothTokens() throws Exception {
        when(authService.login("alice", "password123"))
                .thenReturn(new AuthResponse("fake-access", "fake-refresh", "alice", "USER"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-access"))
                .andExpect(jsonPath("$.refreshToken").value("fake-refresh"));
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login("alice", "wrong")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("username", "alice", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns200WithNewTokenPair() throws Exception {
        when(authService.refreshAccessToken("old-refresh-token"))
                .thenReturn(new AuthResponse("new-access", "new-refresh", "alice", "USER"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", "old-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_returns401ForInvalidOrReusedToken() throws Exception {
        when(authService.refreshAccessToken("bad-token")).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", "bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_rejectsBlankRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", ""))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(Map.of("refreshToken", "some-token"))))
                .andExpect(status().isNoContent());

        verify(authService).logout("some-token");
    }
}
