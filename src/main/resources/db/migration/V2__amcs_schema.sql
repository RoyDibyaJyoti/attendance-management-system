-- V2: Core AMCS schema
-- Tables built incrementally — only what the application currently needs.

-- ============================================================
-- USERS (authentication)
-- ============================================================
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'FACULTY')),
    full_name   VARCHAR(200) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- ACADEMIC TERMS
-- ============================================================
CREATE TABLE academic_terms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- DEPARTMENTS
-- ============================================================
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- SECTIONS
-- ============================================================
CREATE TABLE sections (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL,
    department_id  BIGINT       NOT NULL REFERENCES departments(id),
    term_id        BIGINT       NOT NULL REFERENCES academic_terms(id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE(name, department_id, term_id)
);

-- ============================================================
-- SUBJECTS
-- ============================================================
CREATE TABLE subjects (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    type         VARCHAR(10)  NOT NULL CHECK (type IN ('THEORY', 'LAB')),
    credits      INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- FACULTY
-- ============================================================
CREATE TABLE faculty (
    id             BIGSERIAL PRIMARY KEY,
    employee_id    VARCHAR(50)  NOT NULL UNIQUE,
    user_id        BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    department_id  BIGINT       NOT NULL REFERENCES departments(id),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- STUDENTS
-- ============================================================
CREATE TABLE students (
    id                  BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    email               VARCHAR(200),
    department_id       BIGINT       NOT NULL REFERENCES departments(id),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- LAB BATCHES  (belong to a section)
-- ============================================================
CREATE TABLE lab_batches (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    section_id  BIGINT      NOT NULL REFERENCES sections(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(name, section_id)
);

-- ============================================================
-- ENROLLMENTS  (student → section, with optional lab batch)
-- ============================================================
CREATE TABLE enrollments (
    id            BIGSERIAL PRIMARY KEY,
    student_id    BIGINT NOT NULL REFERENCES students(id),
    section_id    BIGINT NOT NULL REFERENCES sections(id),
    lab_batch_id  BIGINT REFERENCES lab_batches(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(student_id, section_id)
);

-- ============================================================
-- TIMETABLE ENTRIES  (recurring planned class)
-- ============================================================
CREATE TABLE timetable_entries (
    id            BIGSERIAL PRIMARY KEY,
    term_id       BIGINT       NOT NULL REFERENCES academic_terms(id),
    section_id    BIGINT       NOT NULL REFERENCES sections(id),
    subject_id    BIGINT       NOT NULL REFERENCES subjects(id),
    faculty_id    BIGINT       NOT NULL REFERENCES faculty(id),
    day_of_week   VARCHAR(10)  NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time    TIME         NOT NULL,
    end_time      TIME         NOT NULL,
    room          VARCHAR(50),
    lab_batch_id  BIGINT       REFERENCES lab_batches(id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- CLASS SESSIONS  (one actual occurrence of a timetable entry)
-- ============================================================
CREATE TABLE class_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    timetable_entry_id  BIGINT      NOT NULL REFERENCES timetable_entries(id),
    session_date        DATE        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'SUBMITTED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at        TIMESTAMPTZ,
    UNIQUE(timetable_entry_id, session_date)
);

-- ============================================================
-- ATTENDANCE RECORDS
-- ============================================================
CREATE TABLE attendance_records (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT      NOT NULL REFERENCES class_sessions(id),
    student_id  BIGINT      NOT NULL REFERENCES students(id),
    status      VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT')),
    marked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(session_id, student_id)
);

-- ============================================================
-- INDEXES for common query patterns
-- ============================================================
CREATE INDEX idx_enrollments_section ON enrollments(section_id);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_timetable_section   ON timetable_entries(section_id);
CREATE INDEX idx_timetable_faculty   ON timetable_entries(faculty_id);
CREATE INDEX idx_sessions_entry      ON class_sessions(timetable_entry_id);
CREATE INDEX idx_sessions_date       ON class_sessions(session_date);
CREATE INDEX idx_attendance_session  ON attendance_records(session_id);
CREATE INDEX idx_attendance_student  ON attendance_records(student_id);
