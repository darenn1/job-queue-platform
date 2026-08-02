package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByApiKeyHash(String apiKeyHash);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}