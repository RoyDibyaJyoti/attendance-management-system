-- ============================================================================
-- AMCS Migration: V3__security_and_authentication.sql
-- Phase 4.1: Database Security Migration
-- Introduces:
--   1. 'user_accounts' table (Identity, credentials, and institutional persona links)
--   2. 'faculty_assignments' table (Teaching scope enforcement)
-- ============================================================================

-- 1. User Accounts (Decoupled identity, credentials, and role mapping)
CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    student_id UUID NULL REFERENCES students(id) ON DELETE RESTRICT,
    faculty_id UUID NULL REFERENCES faculty(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    token_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_role_links CHECK (
        (role = 'STUDENT'
            AND student_id IS NOT NULL
            AND faculty_id IS NULL)
        OR
        (role = 'FACULTY'
            AND faculty_id IS NOT NULL
            AND student_id IS NULL)
        OR
        (role = 'HOD_ADMIN'
            AND student_id IS NULL)
    ),
    CONSTRAINT chk_user_status CHECK (
        status IN ('ACTIVE', 'LOCKED', 'SUSPENDED', 'DEACTIVATED')
    ),
    CONSTRAINT chk_user_role CHECK (
        role IN ('STUDENT', 'FACULTY', 'HOD_ADMIN')
    )
);

-- Ensure at most one active user account per institutional student
CREATE UNIQUE INDEX uq_user_student
ON user_accounts (student_id)
WHERE student_id IS NOT NULL;

-- Ensure at most one active user account per institutional faculty member
CREATE UNIQUE INDEX uq_user_faculty
ON user_accounts (faculty_id)
WHERE faculty_id IS NOT NULL;

-- 2. Faculty Teaching Assignments (Enforcing Teaching Scope in Application Layer)
CREATE TABLE faculty_assignments (
    id UUID PRIMARY KEY,
    faculty_id UUID NOT NULL REFERENCES faculty(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    academic_period_id UUID NOT NULL REFERENCES academic_periods(id) ON DELETE RESTRICT,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_faculty_subject_section
        UNIQUE (
            faculty_id,
            subject_id,
            section_id,
            academic_period_id
        )
);

-- Fast lookup index for faculty assignment verification during roll-call & scheduling
CREATE INDEX idx_faculty_assignments_lookup
ON faculty_assignments (faculty_id, academic_period_id);
