-- ============================================================================
-- AMCS Migration: V5__import_staged_rows.sql
-- Phase 5.4: Staging Table for Validated Import Rows
-- Supports two-phase import: Stage -> Review -> Commit / Discard
-- ============================================================================

CREATE TABLE import_staged_rows (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_index INT NOT NULL,
    row_type VARCHAR(50) NOT NULL,
    payload_json TEXT NOT NULL,

    CONSTRAINT chk_staged_row_index CHECK (row_index >= 1),
    CONSTRAINT chk_staged_row_type CHECK (
        row_type IN ('STUDENTS', 'SESSIONS', 'ATTENDANCE_RECORDS')
    )
);

CREATE INDEX idx_import_staged_rows_job ON import_staged_rows(job_id);
CREATE INDEX idx_import_staged_rows_job_row ON import_staged_rows(job_id, row_index);
