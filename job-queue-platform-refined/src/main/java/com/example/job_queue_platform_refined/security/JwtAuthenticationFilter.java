package com.example.job_queue_platform_refined.security;

import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticateWithJwt(authHeader.substring("Bearer ".length()));
        } else {
            String apiKeyHeader = request.getHeader("X-API-Key");
            if (apiKeyHeader != null) {
                authenticateWithApiKey(apiKeyHeader);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithJwt(String token) {
        try {
            Claims claims = jwtService.validateAndGetClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            userRepository.findByUsername(username)
                    .ifPresent(user -> setAuthentication(user, role));

        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateWithApiKey(String apiKey) {
        String hashedKey = ApiKeyHasher.hash(apiKey);
        Optional<User> user = userRepository.findByApiKeyHash(hashedKey);
        user.ifPresent(u -> setAuthentication(u, u.getRole().name()));
    }

    private void setAuthentication(User user, String role) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}