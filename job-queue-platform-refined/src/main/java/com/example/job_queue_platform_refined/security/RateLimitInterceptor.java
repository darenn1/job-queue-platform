package com.example.job_queue_platform_refined.security;

import com.example.job_queue_platform_refined.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final int limit;
    private final Duration window;

    public RateLimitInterceptor(RateLimiter rateLimiter,
                                 @Value("${rate-limit.requests-per-window:100}") int limit,
                                 @Value("${rate-limit.window-seconds:60}") long windowSeconds) {
        this.rateLimiter = rateLimiter;
        this.limit = limit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String identifier = resolveIdentifier(request);

        boolean allowed = rateLimiter.isAllowed(identifier, limit, window);
        long remaining = Math.max(0, limit - rateLimiter.getCurrentCount(identifier, window));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            return false; 
        }

        return true;
    }

    private String resolveIdentifier(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "apikey:" + apiKey;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return "user:" + user.getId();
        }

        return "ip:" + request.getRemoteAddr();
    }
}