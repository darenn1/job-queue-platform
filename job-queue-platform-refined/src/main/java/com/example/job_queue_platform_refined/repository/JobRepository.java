package com.example.job_queue_platform_refined.repository;

import com.example.job_queue_platform_refined.domain.Job;
import com.example.job_queue_platform_refined.domain.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);

    @Query("""
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:type IS NULL OR j.type = :type)
            ORDER BY j.createdAt DESC, j.id DESC
            """)
    List<Job> findKeysetFirstPage(@Param("status") JobStatus status,
                                   @Param("type") String type,
                                   Pageable pageable);
 
    @Query("""
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:type IS NULL OR j.type = :type)
              AND (j.createdAt < :lastCreatedAt
                   OR (j.createdAt = :lastCreatedAt AND j.id < :lastId))
            ORDER BY j.createdAt DESC, j.id DESC
            """)
    List<Job> findKeysetAfter(@Param("status") JobStatus status,
                               @Param("type") String type,
                               @Param("lastCreatedAt") Instant lastCreatedAt,
                               @Param("lastId") UUID lastId,
                               Pageable pageable);
 
    @Query(value = """
            SELECT submitted_by, status, COUNT(*) AS job_count
            FROM jobs
            GROUP BY submitted_by, status
            ORDER BY submitted_by, status
            """, nativeQuery = true)
    List<Object[]> findAdminJobSummaryRaw();
} 
