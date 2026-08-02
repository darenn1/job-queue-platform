SELECT j.*, u.email AS submitted_by_email
FROM jobs j
JOIN users u ON j.submitted_by = u.id
WHERE u.id = :userId
  AND j.status = :status
ORDER BY j.created_at DESC
LIMIT 20;