package com.example.job_queue_platform_refined.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("week9")
class JwtServiceTest {

    private static final String TEST_SECRET = "dGhpc2lzYXRlc3RzZWNyZXRrZXlmb3Jqd3R0ZXN0aW5nb25seQ==";

    @Test
    void generateThenValidateRoundTripsTheUsernameAndRole() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);

        String token = jwtService.generateToken("alice", "ADMIN");
        var claims = jwtService.validateAndGetClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void validateThrowsForAnExpiredToken() throws InterruptedException {
        // -1000ms expiration: already expired the instant it's generated.
        JwtService jwtService = new JwtService(TEST_SECRET, -1000);

        String token = jwtService.generateToken("bob", "USER");

        assertThrows(JwtException.class, () -> jwtService.validateAndGetClaims(token));
    }

    @Test
    void validateThrowsForATamperedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);
        String token = jwtService.generateToken("carol", "USER");

        String tampered = token.substring(0, token.length() - 1) +
                (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThrows(JwtException.class, () -> jwtService.validateAndGetClaims(tampered));
    }

    @Test
    void validateThrowsForATokenSignedWithADifferentSecret() {
        JwtService signedWithSecretA = new JwtService(TEST_SECRET, 60_000);
        String token = signedWithSecretA.generateToken("dave", "USER");

        String differentSecret = "ZGlmZmVyZW50c2VjcmV0a2V5Zm9ydGVzdGluZ3B1cnBvc2Vz";
        JwtService validatingWithSecretB = new JwtService(differentSecret, 60_000);

        assertThrows(JwtException.class, () -> validatingWithSecretB.validateAndGetClaims(token));
    }
}