-- ============================================================================
-- AMCS Initial Relational Schema
-- Database Engine: PostgreSQL 16+
-- Flyway Migration: V1__initial_schema.sql
-- ============================================================================

-- 1. Departments
CREATE TABLE departments (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Faculty
CREATE TABLE faculty (
    id UUID PRIMARY KEY,
    employee_id VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Students
CREATE TABLE students (
    id UUID PRIMARY KEY,
    registration_number VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Academic Periods
CREATE TABLE academic_periods (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_period_dates CHECK (end_date >= start_date)
);

-- 5. Sections
CREATE TABLE sections (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    academic_period_id UUID NOT NULL REFERENCES academic_periods(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_section_name_period UNIQUE (department_id, academic_period_id, name)
);

-- 6. Attendance Policies (Versioned & Immutable Historical Snapshots)
CREATE TABLE attendance_policies (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version INT NOT NULL CHECK (version >= 1),
    minimum_threshold_percentage NUMERIC(5,2) NOT NULL CHECK (minimum_threshold_percentage >= 0 AND minimum_threshold_percentage <= 100),
    status_contributions_json TEXT NOT NULL,
    missing_record_strategy VARCHAR(40) NOT NULL DEFAULT 'TREAT_AS_ABSENT',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_policy_name_version UNIQUE (name, version)
);

-- 7. Overall Attendance Policies
CREATE TABLE overall_attendance_policies (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version INT NOT NULL CHECK (version >= 1),
    aggregation_strategy VARCHAR(40) NOT NULL,
    minimum_threshold_percentage NUMERIC(5,2) NOT NULL CHECK (minimum_threshold_percentage >= 0 AND minimum_threshold_percentage <= 100),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_overall_policy_name_version UNIQUE (name, version)
);

-- 8. Subjects
CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    course_type VARCHAR(40) NOT NULL,
    credit_hours INT NOT NULL CHECK (credit_hours >= 0),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Subject Components (For THEORY_INTEGRATED_LABORATORY)
CREATE TABLE subject_components (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    component_type VARCHAR(20) NOT NULL,
    policy_id UUID NOT NULL REFERENCES attendance_policies(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_subject_component UNIQUE (subject_id, component_type)
);

-- 10. Lab Groups
CREATE TABLE lab_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lab_group_name UNIQUE (section_id, name)
);

-- 11. Student Section Enrollments (Temporal History)
CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    enrollment_start DATE NOT NULL,
    enrollment_end DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enrollment_dates CHECK (enrollment_end IS NULL OR enrollment_end >= enrollment_start)
);

-- 12. Lab Group Memberships (Temporal History)
CREATE TABLE lab_group_memberships (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    lab_group_id UUID NOT NULL REFERENCES lab_groups(id) ON DELETE RESTRICT,
    effective_start DATE NOT NULL,
    effective_end DATE NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_membership_dates CHECK (effective_end IS NULL OR effective_end >= effective_start)
);

-- 13. Sessions (Class Occurrences with Planned vs Conducted Units Invariant)
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    conducted_by_faculty_id UUID NOT NULL REFERENCES faculty(id) ON DELETE RESTRICT,
    academic_period_id UUID NOT NULL REFERENCES academic_periods(id) ON DELETE RESTRICT,
    session_date DATE NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    planned_units INT NOT NULL CHECK (planned_units >= 1),
    conducted_units INT NOT NULL CHECK (conducted_units >= 0),
    status VARCHAR(30) NOT NULL,
    lab_group_id UUID NULL REFERENCES lab_groups(id) ON DELETE RESTRICT,
    replaced_by_session_id UUID NULL REFERENCES sessions(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_session_conducted_units CHECK (
        (status = 'CONDUCTED' AND conducted_units >= 1) OR
        (status != 'CONDUCTED' AND conducted_units = 0)
    )
);

-- 14. Attendance Records (Atomic Session-Student Facts)
CREATE TABLE attendance_records (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE RESTRICT,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_session_student UNIQUE (session_id, student_id)
);

-- ============================================================================
-- Performance B-Tree Indexes
-- ============================================================================
CREATE INDEX idx_sessions_lookup ON sessions (subject_id, academic_period_id, session_date);
CREATE INDEX idx_sessions_section ON sessions (section_id, session_date);
CREATE INDEX idx_attendance_records_student ON attendance_records (student_id, session_id);
CREATE INDEX idx_attendance_records_session ON attendance_records (session_id);
CREATE INDEX idx_enrollments_student_active ON enrollments (student_id, enrollment_start, enrollment_end);
CREATE INDEX idx_lab_memberships_student ON lab_group_memberships (student_id, effective_start, effective_end);
