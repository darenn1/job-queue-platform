package com.example.job_queue_platform_refined.security;

import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week10")
class RateLimitInterceptorTest {

    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsRequestAndSetsRateLimitHeaders_whenUnderTheLimit() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, 100, 60);
        when(request.getHeader("X-API-Key")).thenReturn("test-key");
        when(rateLimiter.isAllowed("apikey:test-key", 100, Duration.ofSeconds(60))).thenReturn(true);
        when(rateLimiter.getCurrentCount("apikey:test-key", Duration.ofSeconds(60))).thenReturn(5L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "95");
        verify(response, never()).setStatus(429);
    }

    @Test
    void rejectsRequestWithHeaders_whenOverTheLimit() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, 100, 60);
        when(request.getHeader("X-API-Key")).thenReturn("test-key");
        when(rateLimiter.isAllowed("apikey:test-key", 100, Duration.ofSeconds(60))).thenReturn(false);
        when(rateLimiter.getCurrentCount("apikey:test-key", Duration.ofSeconds(60))).thenReturn(100L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "must short-circuit — controller logic should never run");
        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
    }

    @Test
    void usesApiKeyHeaderAsIdentifier_whenPresent() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, 100, 60);
        when(request.getHeader("X-API-Key")).thenReturn("my-api-key");
        when(rateLimiter.isAllowed(eq("apikey:my-api-key"), anyInt(), any())).thenReturn(true);
        when(rateLimiter.getCurrentCount(eq("apikey:my-api-key"), any())).thenReturn(0L);

        interceptor.preHandle(request, response, new Object());

        verify(rateLimiter).isAllowed(eq("apikey:my-api-key"), anyInt(), any());
    }

    @Test
    void fallsBackToAuthenticatedUserId_whenNoApiKeyHeaderPresent() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, 100, 60);
        when(request.getHeader("X-API-Key")).thenReturn(null);

        User user = new User("alice", "alice@example.com", "hash", Role.USER);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(rateLimiter.isAllowed(startsWith("user:"), anyInt(), any())).thenReturn(true);
        when(rateLimiter.getCurrentCount(startsWith("user:"), any())).thenReturn(0L);

        interceptor.preHandle(request, response, new Object());

        verify(rateLimiter).isAllowed(startsWith("user:"), anyInt(), any());
    }

    @Test
    void fallsBackToClientIp_whenNoApiKeyAndNotAuthenticated() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiter, 100, 60);
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiter.isAllowed("ip:127.0.0.1", 100, Duration.ofSeconds(60))).thenReturn(true);
        when(rateLimiter.getCurrentCount("ip:127.0.0.1", Duration.ofSeconds(60))).thenReturn(0L);

        interceptor.preHandle(request, response, new Object());

        verify(rateLimiter).isAllowed("ip:127.0.0.1", 100, Duration.ofSeconds(60));
    }
}