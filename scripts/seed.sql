TRUNCATE TABLE jobs, users CASCADE;

INSERT INTO users (username, email, password_hash, role, created_at) VALUES
    ('alice', 'alice@example.com', 'placeholder-hash-1', 'USER', now()),
    ('bob',   'bob@example.com',   'placeholder-hash-2', 'USER', now()),
    ('carol', 'carol@example.com', 'placeholder-hash-3', 'ADMIN', now());

INSERT INTO jobs (type, payload, status, priority, retry_count)
SELECT
    (ARRAY['send_email', 'resize_image', 'generate_report'])[floor(random() * 3 + 1)],
    '{}',
    (ARRAY['PENDING', 'RUNNING', 'COMPLETED', 'FAILED'])[floor(random() * 4 + 1)],
    floor(random() * 5)::int,
    floor(random() * 3)::int,
FROM generate_series(1, 200);

