ALTER TABLE jobs ADD COLUMN IF NOT EXISTS submitted_by UUID REFERENCES users(id);
 
CREATE INDEX IF NOT EXISTS idx_jobs_submitted_by ON jobs (submitted_by);