-- ============================================================================
-- AMCS Migration V7: Timetable and Session Times
-- ============================================================================

-- 1. Create Custom Time Range Type
CREATE TYPE timerange AS RANGE (
    subtype = time
);

-- 2. Create Timetable Entries (Recurring Weekly Schedule)
CREATE TABLE timetable_entries (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    academic_period_id UUID NOT NULL REFERENCES academic_periods(id) ON DELETE RESTRICT,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    planned_units INT NOT NULL CHECK (planned_units >= 1),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_timetable_times CHECK (start_time < end_time)
);

-- Ensure a section does not have overlapping timetable entries on the same day
ALTER TABLE timetable_entries
    ADD CONSTRAINT uq_timetable_overlap EXCLUDE USING gist (
        section_id WITH =,
        academic_period_id WITH =,
        day_of_week WITH =,
        timerange(start_time, end_time, '[)') WITH &&
    ) WHERE (is_active = true);

-- 2. Extend Sessions with Timetable Link and Time Bounds
ALTER TABLE sessions ADD COLUMN timetable_entry_id UUID NULL REFERENCES timetable_entries(id) ON DELETE RESTRICT;
ALTER TABLE sessions ADD COLUMN start_time TIME NULL;
ALTER TABLE sessions ADD COLUMN end_time TIME NULL;
ALTER TABLE sessions ADD CONSTRAINT chk_session_times CHECK (
    (start_time IS NULL AND end_time IS NULL) OR 
    (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
);

-- Idempotency constraint: Prevent generating duplicate sessions for the same timetable entry on the same date
CREATE UNIQUE INDEX uq_session_timetable_date 
ON sessions (timetable_entry_id, session_date) 
WHERE timetable_entry_id IS NOT NULL;

-- Ensure a faculty member cannot be double-booked for two sessions at the exact same time
ALTER TABLE sessions
    ADD CONSTRAINT uq_faculty_session_overlap EXCLUDE USING gist (
        conducted_by_faculty_id WITH =,
        session_date WITH =,
        timerange(start_time, end_time, '[)') WITH &&
    ) WHERE (start_time IS NOT NULL AND end_time IS NOT NULL AND status != 'CANCELLED');
