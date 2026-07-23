ALTER TABLE jobs ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

UPDATE jobs SET retry_count = floor(random() * 4)::int WHERE status = 'FAILED';

WITH failed_counts AS (
    SELECT type, COUNT(*) AS failed_count
    FROM jobs
    WHERE status = 'FAILED'
    GROUP BY type
)
SELECT * FROM failed_counts
ORDER BY failed_count DESC;

SELECT DISTINCT type
FROM jobs j
WHERE status = 'FAILED'
  AND created_at = (
      SELECT MAX(created_at)
      FROM jobs j2
      WHERE j2.type = j.type
  );

WITH most_recent_job_per_type AS (
    SELECT
        type,
        status,
        ROW_NUMBER() OVER (PARTITION BY type ORDER BY created_at DESC) AS rn
    FROM jobs
)
SELECT type
FROM most_recent_job_per_type
WHERE rn = 1
  AND status = 'FAILED';

SELECT id, type, retry_count
FROM jobs
WHERE retry_count > 2
ORDER BY retry_count DESC;

WITH heavily_retried_jobs AS (
    SELECT id, type, retry_count
    FROM jobs
    WHERE retry_count > 2
)
SELECT
    type,
    COUNT(*) AS heavily_retried_count,
    AVG(retry_count) AS avg_retry_count
FROM heavily_retried_jobs
GROUP BY type
ORDER BY heavily_retried_count DESC;

WITH failed_jobs AS (
    SELECT type, retry_count
    FROM jobs
    WHERE status = 'FAILED'
),
retry_stats_by_type AS (
    SELECT
        type,
        COUNT(*) AS total_failed,
        AVG(retry_count) AS avg_retries_before_failing
    FROM failed_jobs
    GROUP BY type
)
SELECT *
FROM retry_stats_by_type
WHERE total_failed > 5
ORDER BY avg_retries_before_failing DESC;