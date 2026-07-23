SELECT
    id,
    type,
    created_at,
    ROW_NUMBER() OVER (PARTITION BY type ORDER BY created_at ASC) AS job_sequence_number
FROM jobs
ORDER BY type, job_sequence_number;


SELECT
    id,
    type,
    priority,
    RANK() OVER (PARTITION BY type ORDER BY priority DESC) AS priority_rank
FROM jobs
ORDER BY type, priority_rank;

SELECT
    id,
    type,
    created_at,
    LAG(created_at) OVER (PARTITION BY type ORDER BY created_at ASC) AS previous_job_created_at,
    created_at - LAG(created_at) OVER (PARTITION BY type ORDER BY created_at ASC) AS time_since_previous_job
FROM jobs
ORDER BY type, created_at;


SELECT
    id,
    type,
    created_at,
    LEAD(created_at) OVER (PARTITION BY type ORDER BY created_at ASC) AS next_job_created_at
FROM jobs
ORDER BY type, created_at;

SELECT *
FROM (
    SELECT
        id,
        type,
        status,
        created_at,
        ROW_NUMBER() OVER (PARTITION BY type ORDER BY created_at DESC) AS rn
    FROM jobs
) ranked
WHERE rn = 1;