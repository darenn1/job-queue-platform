CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO users (username, email) VALUES
('alice', 'alice@example.com'),
('bob', 'bob@example.com'),
('carol', 'carol@example.com');

ALTER TABLE jobs ADD COLUMN user_id INTEGER REFERENCES users(id);

UPDATE jobs SET user_id = (id % 3) + 1;

SELECT j.id, j.title, j.status, u.username
FROM jobs j
INNER JOIN users u ON j.user_id = u.id
ORDER BY u.username;

SELECT u.username, COUNT(j.id) AS job_count
FROM users u
LEFT JOIN jobs j ON j.user_id = u.id
GROUP BY u.username
ORDER BY job_count DESC;

SELECT status, COUNT(*) AS total
FROM jobs
GROUP BY status
ORDER BY total DESC;

SELECT u.username, COUNT(j.id) AS failed_count
FROM users u
JOIN jobs j ON j.user_id = u.id
WHERE j.status = 'FAILED'
GROUP BY u.username
HAVING COUNT(j.id) > 0
ORDER BY failed_count DESC
LIMIT 1;