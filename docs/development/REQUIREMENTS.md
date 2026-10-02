# Attendance Management and Calculation System
# REQUIREMENTS.md — Phase 0

**Document Status:** Draft — Awaiting Institutional Confirmation  
**Version:** 0.1.0  
**Date:** 2026-08-29  
**Classification:** Internal — Architecture & Planning

---

## Table of Contents

1. [Document Purpose](#1-document-purpose)
2. [Actors and Roles](#2-actors-and-roles)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Core Entities and Relationships](#5-core-entities-and-relationships)
6. [Data Integrity Requirements](#6-data-integrity-requirements)
7. [Excel Import / Export Requirements](#7-excel-import--export-requirements)
8. [Security Requirements](#8-security-requirements)
9. [Audit Requirements](#9-audit-requirements)
10. [Reporting Requirements](#10-reporting-requirements)
11. [Known Ambiguities](#11-known-ambiguities)
12. [Edge Cases Identified](#12-edge-cases-identified)

---

## 1. Document Purpose

This document formally captures the requirements for the Attendance Management and Calculation System (AMCS) intended for deployment in a college or university setting. It distinguishes:

- **[CONFIRMED]** — Requirements established by project scope or universally applicable technical need.
- **[ASSUMPTION]** — Reasonable technical assumptions made to progress analysis; subject to reversal.
- **[REQUIRES INSTITUTIONAL CONFIRMATION]** — Rules, thresholds, or policies that vary by institution and MUST be answered before Phase 1 implementation begins.

---

## 2. Actors and Roles

### 2.1 Primary Actors

| Actor | Description |
|---|---|
| **Super Admin** | Has full system access. Manages institutions, academic years, and global configuration. |
| **Department Admin** | Manages configuration within their department: subjects, faculty assignment, section structure. |
| **Faculty / Instructor** | Marks attendance for sessions they conduct. Views own subject-level reports. |
| **Student** | Views their own attendance records, percentages, and shortage/prediction data. |
| **HOD (Head of Department)** | Views department-wide attendance analytics and reports. May approve corrections. |
| **Exam / Academic Section Staff** | Generates formal attendance reports, exports for board/university submission. |
| **System / Scheduler** | Automated actor: triggers attendance calculation jobs, sends notifications. |

### 2.2 Role Hierarchy

```
Super Admin
  └── Department Admin
        ├── HOD
        ├── Faculty
        └── Exam/Academic Staff
              └── (read access only: Student)
```

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Does the institution use a flat role model or a hierarchical delegation model? Are there co-admin arrangements?

### 2.3 Role Permissions Summary

| Action | Super Admin | Dept Admin | HOD | Faculty | Exam Staff | Student |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Manage global config | Y | — | — | — | — | — |
| Manage departments | Y | — | — | — | — | — |
| Manage academic year | Y | Y | — | — | — | — |
| Create/edit subjects | Y | Y | — | — | — | — |
| Assign faculty | Y | Y | — | — | — | — |
| Mark attendance | Y | Y | — | Y | — | — |
| Edit past attendance | Y | Y | Y | limited | — | — |
| View dept reports | Y | Y | Y | own | Y | own |
| Export reports | Y | Y | Y | — | Y | — |
| Import attendance | Y | Y | — | Y | — | — |
| Audit log view | Y | Y | — | — | — | — |

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Exact permission boundaries, especially for faculty editing past attendance.

---

## 3. Functional Requirements

### 3.1 Academic Structure Management

**FR-001** — The system SHALL support a multi-level academic hierarchy:
Institution → Academic Year → Department → Programme → Section/Batch → Student

**FR-002** — The system SHALL support multiple academic years and MUST prevent cross-year data contamination.

**FR-003** — Each Section SHALL have a defined set of enrolled students, and enrollment MUST be versioned (students may be transferred between sections).

**FR-004** — Each Subject SHALL be associated with a Department, an Academic Year, and one or more Sections.

**FR-005** — Each Subject SHALL have a type: THEORY, LABORATORY, or THEORY_INTEGRATED_LABORATORY.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Are there other course types (e.g., tutorial, seminar, fieldwork, project)?

**FR-006** — A Subject SHALL have at minimum one assigned Faculty per Section. Multiple faculty per subject per section SHALL be supported (e.g., for labs with divided groups).

**FR-007** — The system SHALL support an "Academic Calendar" that defines holidays, cancelled days, and extra working days, as these affect session scheduling and shortage computation.

### 3.2 Session Management

**FR-010** — A Session represents a single class occurrence (one theory class, one lab period, or one lab session spanning multiple periods).

**FR-011** — Sessions SHALL be created either:
  (a) Manually by faculty or admin, or
  (b) Generated from a Timetable (if timetable module is integrated).

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Is timetable management in scope for this system, or does the institution maintain it externally?

**FR-012** — A Session SHALL record: subject, section, faculty, date, start time, end time, session type (THEORY / LAB), and how many conducted units it represents.

**FR-013** — The number of conducted units per session SHALL be a configurable field, not always 1. A lab session may represent 2 or 3 periods worth of units.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Does a single lab session count as 1 attendance event, or as N units (where N = number of periods occupied)?

**FR-014** — Sessions SHALL have statuses: SCHEDULED, CONDUCTED, CANCELLED, RESCHEDULED.

**FR-015** — A cancelled session MUST NOT affect the denominator (total conducted units) in attendance calculation.

**FR-016** — Sessions marked RESCHEDULED SHALL reference the replacement session.

### 3.3 Attendance Recording

**FR-020** — For each CONDUCTED session, the faculty SHALL record an attendance entry per student: PRESENT, ABSENT, or DUTY_LEAVE / MEDICAL_LEAVE / ON_DUTY (etc.).

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** What attendance statuses are valid? Does the institution distinguish between authorized and unauthorized absence?

**FR-021** — Attendance MUST be submitted per session. Bulk entry for multiple sessions at once SHALL be supported via the import workflow only, with audit trail.

**FR-022** — Once submitted, attendance for a session SHALL be considered locked after a configurable grace period. Edits after locking SHALL require elevated role approval and SHALL generate audit events.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** What is the grace period for editing submitted attendance? Who approves corrections?

**FR-023** — The system MUST NOT allow marking attendance for a student not enrolled in the section for that subject on that date.

**FR-024** — The system MUST support batch-marking: mark all as present, then selectively mark absences.

**FR-025** — Late submissions (faculty marking attendance days after the class) SHALL be flagged and audited.

### 3.4 Attendance Calculation

**FR-030** — Attendance SHALL be computed on demand (and optionally cached/materialized) from the raw attendance records and session records. It SHALL NEVER be stored as a raw user-entered percentage.

**FR-031** — The system SHALL support configurable Attendance Calculation Policies. A policy encapsulates:
- The counting unit for theory sessions.
- The counting unit for laboratory sessions.
- How attendance status variants (DUTY_LEAVE, MEDICAL_LEAVE, ON_DUTY, etc.) contribute to the numerator.
- The minimum required attendance threshold percentage.
- Whether shortage calculation uses calendar days or session units.
- Whether condonation allowances apply.

**FR-032** — Each Subject SHALL be assigned an Attendance Calculation Policy (which MAY differ between theory and lab types).

**FR-033** — Subject-Level Attendance: For each student, for each subject, for each academic period, the system SHALL compute:
- Total conducted units (denominator)
- Total attended units (numerator)
- Attendance percentage
- Units short (if below threshold)
- Surplus units (if above threshold)

**FR-034** — Overall/General Attendance: After subject-level percentages are computed, a separate Overall Attendance Policy SHALL define how to aggregate them into a single student-level percentage.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Is overall attendance the arithmetic mean of subject percentages, a weighted mean by credit hours, a separate calculation on aggregate hours across all subjects, or something else entirely?

**FR-035** — Future Attendance Prediction: The system SHALL compute, for each student/subject below threshold:
- The minimum number of consecutive future classes they must attend to reach the threshold (assuming no further absences), given a configurable total expected classes count.
- The maximum number of additional classes they can miss while still reaching the threshold.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Is the total expected number of sessions per subject known in advance, or estimated from the timetable?

**FR-036** — Shortage detection SHALL respect configured condonation rules if applicable.

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Does the institution allow medical/duty leave condonation? Is condonation applied before or after threshold comparison? What is the maximum condonation allowance?

**FR-037** — Attendance calculations SHALL be reproducible: given the same session and attendance records and the same policy configuration at any point in time, the system SHALL produce the same result.

**FR-038** — Policy version history MUST be maintained. A change to an Attendance Policy MUST NOT retroactively alter already-reported historical data unless explicitly re-run with audit trail.

### 3.5 Dashboard and Analytics

**FR-040** — Faculty dashboard SHALL display: upcoming sessions, recently marked sessions, and subject-wise attendance summary for assigned sections.

**FR-041** — Student dashboard SHALL display: own subject-wise attendance, overall attendance, shortage status (if applicable), and prediction data.

**FR-042** — HOD/Department Admin dashboard SHALL display: department-wide attendance heat maps, subjects with high absence rates, defaulter lists (students below threshold).

**FR-043** — Super Admin dashboard SHALL display: institution-wide summary, department comparison, data quality indicators (sessions not yet marked, etc.).

**FR-044** — All dashboard widgets displaying attendance percentages SHALL clearly indicate the policy and date range used for computation.

### 3.6 Excel / XLSX Import

**FR-050** — The system SHALL accept XLSX files for:
  - Bulk student enrollment data
  - Bulk session creation
  - Bulk attendance marking

**FR-051** — Import templates SHALL be downloadable from the system. Templates SHALL include column headers, data type hints, and example rows.

**FR-052** — Every import SHALL produce a detailed result report: rows accepted, rows rejected, and per-row validation errors.

**FR-053** — Partial imports SHALL be supported: if row 10 fails validation, rows 1-9 and 11-N that passed validation SHALL still be importable (with explicit confirmation from the user).

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Is partial import acceptable, or must the entire file be valid before any data is committed?

**FR-054** — Imports SHALL be atomic at the confirmed set level: either all confirmed rows are committed or none.

**FR-055** — All imports SHALL be audited: who uploaded, when, filename, rows processed, rows accepted, rows rejected.

### 3.7 Excel / XLSX Export

**FR-060** — The system SHALL export XLSX reports for:
  - Student-wise attendance (per subject, per semester)
  - Subject-wise attendance summary (for a section)
  - Defaulter list (students below threshold)
  - Duty-leave / condonation register
  - Full attendance register (date-wise, student-wise, per subject)

**FR-061** — Exports SHALL support filtering by: academic year, department, programme, section, semester, subject, date range, and attendance status.

**FR-062** — Exported files SHALL include metadata: generated-by, generated-at, filter parameters, and policy version used.

**FR-063** — Large exports SHALL be processed asynchronously and made available for download, with notification to the requesting user.

### 3.8 Notification (Requires Confirmation)

> **[REQUIRES INSTITUTIONAL CONFIRMATION]** Should the system send email/SMS/push notifications to students approaching the shortage threshold? To faculty for unmarked sessions? To HOD for submission deadlines?

**FR-070** — If notifications are in scope, they SHALL be configurable (on/off per event type, per role).

**FR-071** — Notification delivery failures SHALL be logged and retried.

---

## 4. Non-Functional Requirements

### 4.1 Performance

**NFR-001** — Attendance computation for a single student/subject pair SHALL complete within 500 ms.

**NFR-002** — Department-wide defaulter list generation (up to 1000 students) SHALL complete within 10 seconds.

**NFR-003** — XLSX import of up to 5,000 rows SHALL complete within 60 seconds.

**NFR-004** — Dashboard load time SHALL be under 2 seconds for 95th percentile of users under normal load.

**NFR-005** — The system SHALL support at least 200 concurrent authenticated users without degradation.

### 4.2 Scalability

**NFR-010** — The data model SHALL support at minimum: 10,000 students, 500 faculty, 200 subjects, and 5 academic years of data without schema changes.

**NFR-011** — Attendance calculation SHALL be horizontally scalable (computation jobs can run in parallel per section or per subject).

### 4.3 Availability

**NFR-020** — The system SHALL target 99.5% uptime during academic term.

**NFR-021** — Scheduled maintenance windows SHALL not occur during peak usage hours (exam preparation and result periods).

### 4.4 Data Integrity

**NFR-030** — All writes to attendance-related tables SHALL be transactional (ACID).

**NFR-031** — Database constraints (foreign keys, unique constraints, check constraints) SHALL enforce referential integrity at the database level, not only at the application layer.

**NFR-032** — No attendance record SHALL exist for a student not enrolled in the corresponding subject-section for the corresponding academic period.

### 4.5 Security

**NFR-040** — All endpoints SHALL require authentication. There SHALL be no publicly accessible attendance data.

**NFR-041** — Authorization SHALL be enforced at the service layer, not only at the API gateway.

**NFR-042** — Sensitive data (passwords) SHALL be stored using a strong adaptive hashing algorithm (bcrypt/Argon2). Plaintext passwords SHALL never be stored or logged.

**NFR-043** — All API communication SHALL use HTTPS in production.

**NFR-044** — JWT tokens (or session tokens) SHALL have configurable expiry. Refresh token rotation SHALL be implemented.

**NFR-045** — The system SHALL enforce rate limiting on authentication endpoints.

### 4.6 Auditability

**NFR-050** — Every mutation (create, update, delete) to core domain entities SHALL produce an audit log entry.

**NFR-051** — Audit logs SHALL be immutable once written.

**NFR-052** — Audit logs SHALL be retained for at least one academic year beyond the current one.

### 4.7 Maintainability

**NFR-060** — Attendance calculation rules SHALL be implemented as explicit policy objects, not scattered if/else conditions.

**NFR-061** — The system SHALL achieve at least 80% unit test coverage on service/domain layer code.

**NFR-062** — All public APIs SHALL be documented via OpenAPI 3.x specification.

**NFR-063** — The codebase SHALL follow a layered architecture: API -> Service -> Domain -> Repository, with no layer bypassing the one above it.

### 4.8 Testability

**NFR-070** — Domain logic (attendance policy evaluation) SHALL be independently unit-testable without a database or network.

**NFR-071** — Integration tests SHALL use Testcontainers with a real PostgreSQL instance.

**NFR-072** — Import/export logic SHALL be testable with fixture XLSX files.

### 4.9 Observability

**NFR-080** — The system SHALL expose structured logs (JSON format) with request correlation IDs.

**NFR-081** — The system SHALL expose health check endpoints compatible with Kubernetes/Docker liveness and readiness probes.

**NFR-082** — Key metrics (request latency, import job duration, calculation job duration) SHALL be exposed in a Prometheus-compatible format.

---

## 5. Core Entities and Relationships

### 5.1 Entity Overview

```
Institution
  └── AcademicYear
        └── Department
              └── Programme
                    └── Section
                          ├── StudentEnrollment  (Student <-> Section, versioned)
                          └── SubjectSection     (Subject <-> Section <-> Faculty)

Subject
  ├── SubjectType (THEORY | LAB | THEORY_INTEGRATED_LAB)
  └── AttendancePolicy (reference)

Session
  ├── Subject
  ├── Section
  ├── Faculty (who conducted)
  ├── SessionType (THEORY | LAB)
  ├── ConductedUnits (integer >= 1)
  └── Status (SCHEDULED | CONDUCTED | CANCELLED | RESCHEDULED)

AttendanceRecord
  ├── Session
  ├── Student
  └── Status (PRESENT | ABSENT | DUTY_LEAVE | MEDICAL_LEAVE | ON_DUTY | ...)

AttendancePolicy
  ├── TheoryUnitWeight
  ├── LabUnitWeight
  ├── MinimumThresholdPercentage
  ├── StatusContributionMap (which statuses count toward numerator)
  └── CondonationRules (optional)

OverallAttendancePolicy
  └── AggregationStrategy (ARITHMETIC_MEAN | WEIGHTED_BY_CREDITS | AGGREGATE_HOURS | ...)

AuditLog
  ├── ActorId, EntityType, EntityId
  ├── ChangeType (CREATE | UPDATE | DELETE)
  ├── OldValue (JSON), NewValue (JSON)
  └── Timestamp, CorrelationId

ImportJob
  ├── UploadedBy, UploadedAt, Filename, ImportType
  ├── Status (PENDING | PROCESSING | COMPLETED | FAILED | PARTIAL)
  └── RowsTotal, RowsAccepted, RowsRejected
```

### 5.2 Key Relationships

| Relationship | Cardinality | Notes |
|---|---|---|
| Student <-> Section | Many-to-Many via Enrollment | Enrollment versioned with start/end dates |
| Subject <-> Section | Many-to-Many via SubjectSection | One subject across multiple sections |
| Subject <-> Faculty | Many-to-Many per Section | Multiple faculty may share a subject |
| Session -> Subject | Many-to-One | A session belongs to one subject |
| AttendanceRecord -> Session | Many-to-One | One record per student per session |
| AttendanceRecord -> Student | Many-to-One | A student has many records |
| Subject -> AttendancePolicy | Many-to-One | Policy is reusable across subjects |

---

## 6. Data Integrity Requirements

**DI-001** — An AttendanceRecord SHALL NOT exist for a student not enrolled in the subject-section for the session's date.

**DI-002** — A Session SHALL NOT be CONDUCTED without at least one AttendanceRecord (unless faculty explicitly confirms full section absence).

**DI-003** — A student's enrollment start/end dates SHALL gate which sessions count in their attendance denominator.

**DI-004** — Changing a session's status from CONDUCTED to CANCELLED MUST be audited and MUST invalidate any cached attendance computations for affected students.

**DI-005** — Duplicate sessions (same subject, section, date, time) SHALL be rejected.

**DI-006** — Duplicate attendance records (same student, same session) SHALL be rejected at the database level via unique constraint.

**DI-007** — Faculty can only mark attendance for sessions assigned to them, or with admin override (audited).

**DI-008** — Academic year boundaries SHALL be enforced: sessions and enrollments MUST fall within the defined academic year date range.

---

## 7. Excel Import / Export Requirements

### 7.1 Import Risks

**IR-001 — Duplicate Detection:** Imported rows may duplicate existing records. The system must perform idempotency checks.

**IR-002 — Invalid References:** Rows referencing nonexistent students, subjects, or sections MUST be rejected with clear error messages.

**IR-003 — Date/Time Parsing Ambiguity:** Excel dates are floating-point serial numbers. The system MUST parse using strict cell type checks and reject ambiguous text-format dates.

**IR-004 — Character Encoding:** Excel files may contain non-UTF-8 characters. The system must handle encoding safely.

**IR-005 — Large File Attacks:** File size limits and streaming SAX-based parsing MUST be used to prevent memory exhaustion.

**IR-006 — Formula Injection:** Cells starting with =, +, -, @ must be sanitized on export to prevent formula injection in Excel.

**IR-007 — Row Limit:** Excel has a row limit of 1,048,576. Imports exceeding this limit MUST fail gracefully.

**IR-008 — Column Mismatch:** If uploaded file columns differ from the template, the system MUST detect and reject (or use header-name matching, clearly documented).

**IR-009 — Concurrent Imports:** Concurrent imports of the same type for the same section/subject MAY produce race conditions. Import jobs MUST acquire a per-resource lock.

**IR-010 — Authorization in Import:** The import endpoint MUST validate that the uploading user has write permission for all target entities in the file.

### 7.2 Export Requirements

**ER-001** — Exported XLSX MUST use proper data types: dates as Excel date cells, numbers as numeric cells.

**ER-002** — Attendance registers exported as XLSX MUST be paginated by section to remain within Excel row limits.

**ER-003** — Exports MUST be reproducible: same parameters + same data state = same file content.

---

## 8. Security Requirements

**SEC-001** — Authentication SHALL use JWT with short-lived access tokens (default 15 min) and refresh token rotation.

**SEC-002** — All role-based authorization checks SHALL be enforced at the service layer.

**SEC-003** — Students SHALL ONLY access their own attendance records, enforced via authenticated token identity, not request parameter.

**SEC-004** — Faculty SHALL ONLY mark attendance for sessions they are assigned to, unless admin override is in effect (audited).

**SEC-005** — The system SHALL implement OWASP Top 10 mitigations including: input validation, SQL injection prevention via parameterized queries, XSS prevention, CSRF protection, and secure headers.

**SEC-006** — File upload endpoints SHALL validate MIME type, file extension, and content structure before processing.

**SEC-007** — Password reset flows SHALL use time-limited, single-use tokens delivered via secure channel.

**SEC-008** — Audit log access SHALL be restricted to Super Admin and Department Admin roles.

**SEC-009** — Rate limiting SHALL be applied to: login (by IP and by username), password reset, and file import endpoints.

**SEC-010** — All API responses SHALL include appropriate security headers (HSTS, X-Content-Type-Options, X-Frame-Options, CSP).

---

## 9. Audit Requirements

**AUD-001** — The following events SHALL produce audit records:

| Event | Required Fields |
|---|---|
| User login/logout | userId, IP, timestamp, success/failure |
| Attendance record created | sessionId, studentId, status, markedBy, timestamp |
| Attendance record modified | sessionId, studentId, oldStatus, newStatus, modifiedBy, reason, timestamp |
| Session status changed | sessionId, oldStatus, newStatus, changedBy, timestamp |
| Attendance policy changed | policyId, oldConfig, newConfig, changedBy, timestamp |
| Import job submitted/completed/failed | jobId, submittedBy, filename, rowCounts, timestamp |
| Export generated | exportType, params, generatedBy, timestamp |
| Student enrollment changed | studentId, sectionId, changeType, changedBy, timestamp |
| Faculty assignment changed | subjectId, sectionId, facultyId, changeType, changedBy, timestamp |
| Role/permission changed | targetUserId, oldRole, newRole, changedBy, timestamp |

**AUD-002** — Audit log entries SHALL be write-once. No UPDATE or DELETE SHALL be permitted on audit records.

**AUD-003** — Audit logs SHALL store old and new values as JSON snapshots. Diffing is a display concern.

**AUD-004** — The system SHALL provide a UI for authorized roles to query audit logs with filters.

**AUD-005** — Audit logs MUST include a correlation ID linking all audit events generated within a single request or import job.

---

## 10. Reporting Requirements

**RPT-001 — Student Attendance Report:** Per-student, per-subject, per-semester. Columns: Subject, Type, Conducted, Attended, Percentage, Status.

**RPT-002 — Subject Attendance Summary:** Per-subject, per-section. Columns: Student Roll, Name, Conducted, Attended, Percentage, Status.

**RPT-003 — Defaulter Report:** Students below threshold. Columns: Roll, Name, Subject, Percentage, Units Short, Classes Required.

**RPT-004 — Attendance Register:** Date-wise attendance grid. Rows: Students. Columns: Dates of conducted sessions. Cells: P/A/DL/etc.

**RPT-005 — Overall Attendance Summary:** Per-student overall attendance using the configured overall policy.

**RPT-006 — Faculty Marking Compliance Report:** Sessions marked vs. unmarked, by faculty and subject.

**RPT-007 — Condonation / Duty Leave Register:** All records with DUTY_LEAVE or MEDICAL_LEAVE status, for administrative verification.

**RPT-008 — Prediction Report:** For students below threshold, required future attendance to reach threshold.

---

## 11. Known Ambiguities

Refer to DOMAIN_RULES.md for the complete list of attendance rule questions requiring institutional confirmation.

---

## 12. Edge Cases Identified

**EC-001 — Student Mid-Semester Transfer:** Student moved between sections. Attendance in original section up to transfer date must be preserved separately.

**EC-002 — Faculty Substitution:** Session conducted by substitute faculty. Attendance still recorded against correct subject/section.

**EC-003 — Retroactive Session Cancellation:** Session already marked CONDUCTED with attendance is later cancelled. Impact on denominator must be defined.

**EC-004 — Subject Reassignment:** Subject reassigned to new faculty mid-semester. Already conducted sessions retain original faculty attribution.

**EC-005 — Zero-Session Period:** Student on approved leave for entire month. Policy question: are those sessions excluded from their denominator?

**EC-006 — Integrated Theory-Lab Subject:** Subject with both theory and lab components has two session streams with different policies. Combined attendance must be computed as a composite.

**EC-007 — Student Late Enrollment:** Student joins weeks after semester start. They MUST NOT be penalized for sessions before their enrollment date.

**EC-008 — Dual-Section Lab Split:** Large section split into Group A and Group B for labs. Each group's lab attendance must be tracked separately.

**EC-009 — Make-up / Compensatory Classes:** Extra class held to compensate for a holiday. Does it count toward denominator? Is student attendance optional?

**EC-010 — Incorrect Bulk Import Committed:** An import was fully committed but contained errors discovered after the fact. A correction workflow must exist that preserves the audit trail.

---

*End of REQUIREMENTS.md*
