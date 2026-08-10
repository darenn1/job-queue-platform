package com.example.job_queue_platform_refined.api.dto;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String username;
    private final String role;

    public AuthResponse(String accessToken, String refreshToken, String username, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }
 
    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}