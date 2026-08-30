-- ============================================================================
-- AMCS Migration: V4__import_jobs_and_staging.sql
-- Phase 5.2: Persistence Migration for Import Jobs and Error Staging
-- Introduces:
--   1. 'import_jobs' table (Metadata, metrics, and lifecycle state tracking)
--   2. 'import_job_errors' table (Row-level validation error diagnostics)
-- ============================================================================

CREATE TABLE import_jobs (
    id UUID PRIMARY KEY,
    import_type VARCHAR(50) NOT NULL,
    import_mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    invalid_rows INT NOT NULL DEFAULT 0,
    created_by_user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    committed_at TIMESTAMPTZ NULL,
    discarded_at TIMESTAMPTZ NULL,

    CONSTRAINT chk_import_type CHECK (
        import_type IN ('STUDENTS', 'SESSIONS', 'ATTENDANCE_RECORDS')
    ),
    CONSTRAINT chk_import_mode CHECK (
        import_mode IN ('FAIL_FAST', 'PARTIAL_COMMIT')
    ),
    CONSTRAINT chk_import_status CHECK (
        status IN ('SUBMITTED', 'STAGED_CLEAN', 'STAGED_PARTIAL', 'REJECTED', 'COMMITTED', 'DISCARDED')
    ),
    CONSTRAINT chk_import_row_counts CHECK (
        total_rows >= 0 AND valid_rows >= 0 AND invalid_rows >= 0
    )
);

CREATE TABLE import_job_errors (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_index INT NOT NULL,
    column_name VARCHAR(100) NOT NULL DEFAULT '',
    rejected_value TEXT NOT NULL DEFAULT '',
    error_code VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,

    CONSTRAINT chk_row_index CHECK (row_index >= 1)
);

CREATE INDEX idx_import_jobs_status ON import_jobs(status);
CREATE INDEX idx_import_jobs_created_by ON import_jobs(created_by_user_id);
CREATE INDEX idx_import_job_errors_job ON import_job_errors(job_id);
CREATE INDEX idx_import_job_errors_row ON import_job_errors(job_id, row_index);
