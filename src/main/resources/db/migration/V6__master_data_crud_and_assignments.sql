-- ============================================================================
-- AMCS Migration V6: Master Data Soft Deletes & Faculty Assignments
-- ============================================================================

-- 1. Add is_active to Master Data Tables
ALTER TABLE departments ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE faculty ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE students ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE sections ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE subjects ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Create Temporal Faculty Assignments Table
CREATE TABLE faculty_section_assignments (
    id UUID PRIMARY KEY,
    faculty_id UUID NOT NULL REFERENCES faculty(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    academic_period_id UUID NOT NULL REFERENCES academic_periods(id) ON DELETE RESTRICT,
    assignment_start DATE NOT NULL,
    assignment_end DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assignment_dates CHECK (assignment_end IS NULL OR assignment_end >= assignment_start)
);

CREATE INDEX idx_faculty_section_assignments_lookup ON faculty_section_assignments (faculty_id, assignment_start, assignment_end);
CREATE INDEX idx_section_assignments_lookup ON faculty_section_assignments (section_id, subject_id, assignment_start, assignment_end);

-- 3. Temporal Overlap Invariant
-- To prevent two faculties from being assigned to the same subject/section at the same time
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE faculty_section_assignments
    ADD CONSTRAINT uq_temporal_assignment EXCLUDE USING gist (
        subject_id WITH =,
        section_id WITH =,
        academic_period_id WITH =,
        daterange(assignment_start, COALESCE(assignment_end, 'infinity'::date), '[]') WITH &&
    );
