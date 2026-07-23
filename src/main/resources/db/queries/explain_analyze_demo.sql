EXPLAIN ANALYZE
SELECT * FROM jobs WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);

EXPLAIN ANALYZE
SELECT * FROM jobs WHERE status = 'PENDING';

EXPLAIN ANALYZE
INSERT INTO jobs (id, type, payload, status, priority, created_at, updated_at)
VALUES (gen_random_uuid(), 'send_email', '{}', 'PENDING', 0, NOW(), NOW());

CREATE INDEX IF NOT EXISTS idx_jobs_priority_status ON jobs (priority, status);

EXPLAIN ANALYZE
SELECT * FROM jobs WHERE priority = 3 AND status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs (created_at);

EXPLAIN ANALYZE
SELECT * FROM jobs ORDER BY created_at DESC LIMIT 20;

CREATE INDEX IF NOT EXISTS idx_jobs_created_at_id ON jobs (created_at DESC, id DESC)