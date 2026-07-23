CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);
 
CREATE INDEX IF NOT EXISTS idx_jobs_priority_status ON jobs (priority, status);
 
CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs (created_at);

CREATE INDEX IF NOT EXISTS idx_jobs_created_at_id ON jobs (created_at DESC, id DESC);