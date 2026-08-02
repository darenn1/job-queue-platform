package com.example.job_queue_platform_refined.integration;

import com.example.job_queue_platform_refined.domain.Role;
import com.example.job_queue_platform_refined.domain.User;
import com.example.job_queue_platform_refined.repository.UserRepository;
import com.example.job_queue_platform_refined.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("week9")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private String tokenFor(String username, Role role) {
        userRepository.save(new User(username, username + "@example.com",
                passwordEncoder.encode("password123"), role));
        return jwtService.generateToken(username, role.name());
    }


    @Test
    void authEndpoints_remainPublic_withNoToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void jobsEndpoint_rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jobsEndpoint_allowsAuthenticatedUser() throws Exception {
        String token = tokenFor("regular-user", Role.USER);

        mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void jobsEndpoint_allowsAuthenticatedAdmin() throws Exception {
        String token = tokenFor("an-admin", Role.ADMIN);

        mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void jobsEndpoint_rejectsExpiredToken() throws Exception {
        userRepository.save(new User("expired-user", "expired@example.com",
                passwordEncoder.encode("password123"), Role.USER));

        JwtService shortLivedJwtService = new JwtService(jwtSecret, -1000);
        String expiredToken = shortLivedJwtService.generateToken("expired-user", "USER");

        mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void workersStatus_rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/workers/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workersStatus_rejectsAuthenticatedNonAdminUser_with403() throws Exception {
        String token = tokenFor("regular-user-2", Role.USER);

        mockMvc.perform(get("/workers/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void workersStatus_allowsAuthenticatedAdmin() throws Exception {
        String token = tokenFor("real-admin", Role.ADMIN);

        mockMvc.perform(get("/workers/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}