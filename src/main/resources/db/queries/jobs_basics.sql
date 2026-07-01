CREATE TABLE jobs (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO jobs (title, status) VALUES
('Send welcome email', 'PENDING'),
('Resize profile image', 'PENDING'),
('Generate invoice #1001', 'COMPLETED'),
('Sync inventory', 'FAILED'),
('Backup database', 'COMPLETED'),
('Process payment #44', 'PENDING'),
('Send password reset', 'COMPLETED'),
('Export report CSV', 'FAILED'),
('Cleanup temp files', 'PENDING'),
('Notify webhook listener', 'PENDING');

SELECT id, title, status, created_at
FROM jobs
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 5;

UPDATE jobs
SET status = 'COMPLETED'
WHERE id = 1;

DELETE FROM jobs
WHERE id = 4;