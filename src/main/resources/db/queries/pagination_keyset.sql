EXPLAIN ANALYZE
SELECT * FROM jobs
WHERE (created_at, id) < (:lastSeenCreatedAt, :lastSeenId)
ORDER BY created_at DESC, id DESC
LIMIT 20;
