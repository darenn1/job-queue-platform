package com.example.job_queue_platform_refined.security;

import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("week9")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenPopulatesSecurityContextWithUserAndRoleAuthority() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        User user = new User("alice", "alice@example.com", "hash", Role.ADMIN);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.validateAndGetClaims("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("alice");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(user, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeaderLeavesSecurityContextEmpty() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response); // request still proceeds — permitAll handles the rest today
        verifyNoInteractions(jwtService);
    }

    @Test
    void invalidTokenClearsSecurityContextAndStillContinuesTheChain() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn("Bearer garbage-token");
        when(jwtService.validateAndGetClaims("garbage-token")).thenThrow(new JwtException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void validTokenForANowDeletedUserLeavesSecurityContextEmpty() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.validateAndGetClaims("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("ghost-user");
        when(userRepository.findByUsername("ghost-user")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}