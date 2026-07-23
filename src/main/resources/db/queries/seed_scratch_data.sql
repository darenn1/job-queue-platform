TRUNCATE TABLE jobs;

INSERT INTO jobs(id, type, payload, status, priority, created_at, updated_at, result)
SELECT
    gen_random_uuid(),
    (ARRAY['send_email', 'resize_image', 'generate_report'])[floor(random() * 3 + 1)],
    '{}',
    (ARRAY['PENDING', 'RUNNING', 'COMPLETED', 'FAILED'])[floor(random() * 4 + 1)],
    floor(random() * 5)::int,
    NOW() - (random() * interval '30 days'),
    NOW() - (random() * interval '30 days'),
    NULL
FROM generate_series(1, 5000);