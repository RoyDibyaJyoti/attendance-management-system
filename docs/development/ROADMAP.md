# Attendance Management and Calculation System
# ROADMAP.md — Phase 0

**Document Status:** Draft  
**Version:** 0.1.0  
**Date:** 2026-08-29  
**Classification:** Internal — Planning

---

## Roadmap Overview

```
Phase 0: Requirements & Architecture [COMPLETE]
    |
Phase 1: Pure Domain Model & Calculation Engine [COMPLETE - 109 tests passing]
    |
Phase 1.5: Domain Audit & Hardening [COMPLETE]
    |
Phase 2: Persistence Architecture & PostgreSQL [COMPLETE]
    |
    ├── Ports & Adapters (Hexagonal isolation of pure domain)
    ├── Flyway Relational Schema (V1__initial_schema.sql)
    ├── PostgreSQL Constraints & Indexing
    ├── Docker Compose & Testcontainers Integration Testing
    |
Phase 3: Application Services, Security Boundaries & REST APIs [COMPLETE - 154 tests]
    |
    ├── Application Services & Transaction Boundaries
    ├── Optimistic Locking & Session State Machine
    ├── Idempotent Batch Roll-Call Recording & Audited Correction
    ├── Pure Domain Calculation Engine Facade & Shortage Projections
    ├── REST Controllers, OpenAPI 3 Documentation & MockMvc Suites
    |
Phase 4: Security, Authentication & RBAC [COMPLETE]
    |
    ├── Phase 4.0: Security Architecture Review [COMPLETE]
    ├── Phase 4.1: Database Security Migration (V3__security_and_authentication.sql) [COMPLETE]
    ├── Phase 4.2: Persistence Entities, Repositories & Adapters [COMPLETE]
    ├── Phase 4.3: Password Hashing & Authentication Service [COMPLETE]
    ├── Phase 4.4: JWT Infrastructure & Token Provider [COMPLETE]
    ├── Phase 4.5: Spring Security Configuration & Filter Chain [COMPLETE]
    ├── Phase 4.6: Application Security Port (CurrentUserPort & AuthenticatedActor) [COMPLETE]
    ├── Phase 4.7: Business Authorization Enforcement (IDOR & Faculty Scope) [COMPLETE]
    ├── Phase 4.8: Security Error Handling Integration (401 & 403 ApiErrorResponse) [COMPLETE]
    ├── Phase 4.9: Comprehensive Security & RBAC Test Suites [COMPLETE]
    └── Phase 4.10: Documentation & Final Security Audit [COMPLETE]
    |
Phase 5: Excel Import/Export & Reporting Pipeline [PENDING]
```

---

## Phase 0 — Requirements Discovery and Architecture Planning

**Status:** In Progress  
**Objective:** Establish the complete requirements baseline and architectural blueprint before writing a single line of production code.

### Deliverables

- [x] REQUIREMENTS.md — Functional and non-functional requirements
- [x] ARCHITECTURE_NOTES.md — High-level architecture, module decomposition, ADRs
- [x] DOMAIN_RULES.md — All attendance rules with explicit classification
- [x] ROADMAP.md — This document

### Success Criteria

- All domain entities are identified.
- All attendance rule questions are explicitly documented.
- The technology stack is confirmed.
- The architectural approach is documented and reviewed.
- All "REQUIRES INSTITUTIONAL CONFIRMATION" items are identified and listed.

### Phase 0 Gate — Must Be Satisfied Before Phase 1 Begins

The following must be completed before Phase 1 begins:

1. All 20 items in the DOMAIN_RULES.md Pre-Implementation Decision Checklist (Section 13) are answered in writing by the institution.
2. The role and permission model is confirmed (flat vs. hierarchical; exact permissions per role).
3. The attendance status list is finalized.
4. The overall attendance aggregation formula is confirmed.
5. The condonation policy (or confirmation that condonation is not used) is documented.
6. The technology stack is approved.
7. At least one domain expert from the institution has reviewed and signed off on DOMAIN_RULES.md.

---

## Phase 1 — Foundation: Project Scaffolding, Authentication, and Academic Structure

**Estimated Complexity:** High (foundational decisions made here affect everything)  
**Prerequisite:** Phase 0 gate satisfied.

### Objective

Build the core infrastructure that all subsequent phases depend on:
- Project structure and build pipeline
- Database schema and migration framework
- Authentication and authorization
- Academic structure management (departments, sections, students, faculty)

### Backend Deliverables

- [ ] Spring Boot 3 project initialized (Maven or Gradle, confirmed in Phase 0)
- [ ] PostgreSQL + Flyway configured; initial schema migration V1
- [ ] Docker Compose with PostgreSQL for local development
- [ ] Application layer structure: `common`, `config`, `academic`, `user`, `audit` modules
- [ ] Flyway migrations for: User, Role, Institution, AcademicYear, Department, Programme, Section, StudentEnrollment
- [ ] JWT authentication (login, token refresh, logout)
- [ ] Spring Security filter chain with role-based method security
- [ ] UserDetailsService with BCrypt password encoding
- [ ] Audit infrastructure: AuditLog entity, write-only repository, AOP aspect
- [ ] CRUD APIs for: Institution, AcademicYear, Department, Programme, Section
- [ ] Student enrollment API (with versioned enrollment dates)
- [ ] Faculty management API
- [ ] Subject management API (with SubjectType and AttendancePolicy assignment)
- [ ] AttendancePolicy entity and CRUD API (policy config, not calculation)
- [ ] OpenAPI spec auto-generated and accessible at /swagger-ui.html
- [ ] Health check endpoints

### Frontend Deliverables

- [ ] Vite + React + TypeScript + Tailwind project initialized
- [ ] API client setup (Axios or Fetch)
- [ ] Authentication flow (login page, token storage in memory + httpOnly cookie for refresh)
- [ ] Protected routing structure
- [ ] Basic layout shell: sidebar navigation per role, header
- [ ] Academic structure management screens (read-only for now; management forms TBD)

### Testing Deliverables

- [ ] JUnit 5 + Testcontainers integration test setup
- [ ] Authentication integration tests (login success, login failure, token refresh, token expiry)
- [ ] Audit log integration tests (verify write-only constraint)
- [ ] Academic structure CRUD tests

### Success Criteria

- A user can log in as Super Admin and receive a valid JWT.
- A Department Admin can log in and only see their department's data.
- A student can log in and only see their own profile.
- Audit events are generated for user login and academic structure changes.
- All migrations run cleanly from scratch.
- Docker Compose spins up the full environment from scratch.

---

## Phase 2 — Session Management and Attendance Recording

**Estimated Complexity:** High  
**Prerequisite:** Phase 1 gate satisfied.

### Objective

Build the ability to create and manage class sessions, and record student attendance for each session.

### Backend Deliverables

- [ ] Session entity + Flyway migration
- [ ] Session CRUD API (create, update status, cancel, reschedule)
- [ ] Session validation: unique constraint, date within academic year, faculty assignment check
- [ ] AttendanceRecord entity + Flyway migration
- [ ] Attendance marking API: mark attendance for a session (per-student status)
- [ ] Batch attendance API: mark-all-present with selective absent marking
- [ ] Attendance lock mechanism (configurable grace period)
- [ ] Post-lock correction API (requires elevated approval, generates approval workflow record)
- [ ] Faculty assignment enforcement (faculty can only mark their assigned sessions)
- [ ] Late submission flagging and audit
- [ ] Academic Calendar API (holidays, extra working days)
- [ ] Data integrity enforcement: enrollment window check on attendance record creation
- [ ] Audit events for all session and attendance record changes

### Frontend Deliverables

- [ ] Faculty session list view (today, upcoming, past)
- [ ] Attendance marking interface (class roster with P/A status selection)
- [ ] Batch mark all present + selective absent interface
- [ ] Session status management (cancel, reschedule)
- [ ] Admin view: all sessions by section/subject

### Testing Deliverables

- [ ] Session creation / validation tests
- [ ] Attendance marking tests (valid cases)
- [ ] Attendance marking tests (invalid cases: wrong student, wrong faculty, outside enrollment window, duplicate)
- [ ] Lock mechanism tests
- [ ] Late submission audit tests

### Success Criteria

- Faculty can create a session and mark attendance for all enrolled students.
- The system rejects: duplicate sessions, out-of-enrollment-window records, duplicate records.
- The lock mechanism prevents editing after the grace period without approval.
- All mutations are audited.

---

## Phase 3 — Calculation Engine, Shortage Analysis, and Reports

**Estimated Complexity:** Very High (domain correctness is paramount here)  
**Prerequisite:** Phase 2 gate satisfied. ALL institutional rule confirmations received.

### Objective

Implement the full attendance calculation engine, shortage detection, future prediction, and core reports.

### Backend Deliverables

- [ ] AttendanceCalculationEngine domain service (pure, no database writes)
- [ ] Subject-level attendance calculation (per-student, per-subject, per-period)
- [ ] Theory/Lab unit weighting from AttendancePolicy
- [ ] StatusContributionMap evaluation (BigDecimal numerator)
- [ ] Enrollment window enforcement in calculation
- [ ] Condonation policy evaluation (if confirmed by institution)
- [ ] Shortage detection per subject
- [ ] OverallAttendancePolicy entity and evaluation (once formula confirmed)
- [ ] Overall attendance calculation
- [ ] PredictionEngine (classes required, classes can miss)
- [ ] Policy version capture in all calculation results
- [ ] Calculation result caching (keyed by policy version + data hash or TTL)
- [ ] Cache invalidation on session/attendance/policy changes
- [ ] Report APIs:
  - Student Attendance Report (per subject)
  - Subject Attendance Summary (per section)
  - Defaulter Report
  - Attendance Register (date-wise grid)
  - Overall Attendance Summary
  - Faculty Marking Compliance Report
  - Prediction Report

### Frontend Deliverables

- [ ] Student dashboard: subject-wise attendance, overall, shortage badge, prediction
- [ ] Faculty view: subject attendance summary for assigned sections
- [ ] HOD/Admin view: defaulter list, section summary

### Testing Deliverables

- [ ] Unit tests for AttendanceCalculationEngine (multiple policy configurations, no DB required)
- [ ] Unit tests for PredictionEngine
- [ ] Unit tests for OverallAttendancePolicy evaluation
- [ ] Unit tests for condonation evaluation
- [ ] Integration tests for end-to-end calculation (sessions in DB -> computed result)
- [ ] Edge case tests: EC-001 through EC-010 from REQUIREMENTS.md
- [ ] Parameterized tests for shortage boundary conditions

### Success Criteria

- Attendance percentages match manually computed values for sample datasets.
- Changing a policy does not retroactively alter previously reported results.
- Prediction engine produces correct results at threshold boundaries.
- All 10 edge cases from REQUIREMENTS.md have corresponding tests.
- BigDecimal is used throughout; no float/double rounding errors.

---

## Phase 4 — Excel Import and Export

**Estimated Complexity:** High  
**Prerequisite:** Phase 3 gate satisfied.

### Objective

Implement the full XLSX import pipeline and all XLSX export reports.

### Backend Deliverables

- [ ] ImportJob entity + status tracking
- [ ] Import pipeline framework: parse -> validate -> stage -> confirm -> commit
- [ ] Streaming XLSX parser (Apache POI SAX/SXSSF) for large file support
- [ ] Import type: Student enrollment bulk import
- [ ] Import type: Session bulk creation
- [ ] Import type: Attendance bulk marking
- [ ] Per-row validation with error accumulation
- [ ] Partial import support (configurable: accept valid rows vs. reject entire file)
- [ ] Duplicate detection per import type
- [ ] Concurrent import locking (per resource type)
- [ ] Import authorization (user must have permission for target entities)
- [ ] Import result report (accepted rows, rejected rows, per-row errors)
- [ ] Import template download endpoints (downloadable template XLSX files)
- [ ] Export service: all 8 report types from RPT-001 through RPT-008
- [ ] Async export for large datasets (JobRecord + download URL)
- [ ] Export metadata embedding (generated-by, generated-at, params, policy version)
- [ ] Formula injection sanitization on all cell values
- [ ] File size limit enforcement on upload
- [ ] MIME type and content validation on upload

### Frontend Deliverables

- [ ] Import wizard: upload -> preview validation result -> confirm -> monitor status
- [ ] Template download buttons per import type
- [ ] Export buttons per report type with filter parameters
- [ ] Async export status tracker (polling or SSE)

### Testing Deliverables

- [ ] Import tests with valid fixture XLSX files
- [ ] Import tests with invalid fixtures (bad dates, missing columns, unknown references, duplicates)
- [ ] Import tests with oversized files
- [ ] Import tests with formula injection cells
- [ ] Export tests: verify output XLSX structure and data correctness
- [ ] Concurrent import race condition tests

### Success Criteria

- A valid XLSX with 5,000 rows imports within 60 seconds.
- An XLSX with mixed valid/invalid rows produces a correct per-row error report.
- Formula injection cells in uploads are rejected or neutralized.
- Exported files contain correct data matching the filtered parameters.
- Import authorization is enforced (faculty cannot import for sections they don't own).

---

## Phase 5 — Dashboard, Analytics, and Notifications

**Estimated Complexity:** Medium  
**Prerequisite:** Phase 4 gate satisfied. Notification channel confirmed by institution.

### Objective

Build all role-specific dashboards with analytics, and implement the notification system if confirmed.

### Backend Deliverables

- [ ] Dashboard data APIs (optimized queries, with caching)
- [ ] Department-wide attendance heat map data
- [ ] High-absence subject detection
- [ ] Faculty marking compliance report API
- [ ] Notification configuration API (event types, channels, on/off per role)
- [ ] Notification trigger service (shortage threshold breached, unmarked session, etc.)
- [ ] Notification delivery service (email and/or SMS, TBD)
- [ ] Notification delivery failure logging and retry
- [ ] Audit log query API with filters

### Frontend Deliverables

- [ ] Super Admin dashboard: institution-wide summary, department comparison, data quality
- [ ] Department Admin / HOD dashboard: section heat maps, defaulter list, compliance
- [ ] Faculty dashboard: upcoming sessions, recently marked, subject summaries
- [ ] Student dashboard: full attendance view, shortage badge, prediction, history
- [ ] Audit log viewer (Super Admin / Dept Admin)
- [ ] Notification preference settings

### Testing Deliverables

- [ ] Dashboard API response time tests (under NFR-004 threshold)
- [ ] Notification delivery tests (mocked delivery channel)
- [ ] Notification failure retry tests

### Success Criteria

- All dashboards load within 2 seconds at 95th percentile.
- Shortage threshold breach triggers a notification (if in scope).
- Unmarked sessions surface in faculty compliance dashboard.
- Audit log viewer shows correct, chronological events.

---

## Phase 6 — Hardening, Performance Tuning, and Production Readiness

**Estimated Complexity:** Medium-High  
**Prerequisite:** All previous phases complete and verified.

### Objective

Make the system production-ready: performance validated, security hardened, documentation complete, deployment automated.

### Deliverables

- [ ] Load testing (k6 or Gatling): validate NFR-001 through NFR-005
- [ ] Database query profiling and index optimization
- [ ] Security audit: OWASP Top 10 checklist, penetration test (at minimum automated scanner)
- [ ] Dependency vulnerability scanning (OWASP Dependency Check or Snyk)
- [ ] Rate limiting implementation and verification
- [ ] Security headers verified (HSTS, CSP, X-Frame-Options, X-Content-Type-Options)
- [ ] Refresh token rotation verification
- [ ] Production Docker Compose / deployment manifests
- [ ] Environment-specific configuration (dev / staging / production profiles)
- [ ] Secrets management strategy (no secrets in codebase or Docker images)
- [ ] Database backup and recovery runbook
- [ ] Operational runbook (startup, shutdown, common failure recovery)
- [ ] Full OpenAPI spec review and cleanup
- [ ] User-facing documentation (if required by institution)
- [ ] Final test coverage report (target: >= 80% service/domain coverage)
- [ ] GitHub Actions CI pipeline: all stages passing on main branch

### Success Criteria

- Load tests pass all NFR thresholds.
- No HIGH or CRITICAL vulnerabilities in dependency scan.
- All OWASP Top 10 items addressed.
- CI pipeline runs cleanly from a fresh checkout.
- The application starts from Docker Compose in under 60 seconds.
- A senior developer can understand the codebase from the README and documentation alone.

---

## Dependency Graph Summary

```
Phase 0 (Requirements)
  └─► Phase 1 (Foundation + Auth)
        └─► Phase 2 (Sessions + Attendance Recording)
              └─► Phase 3 (Calculation Engine + Reports)
                    └─► Phase 4 (Excel Import/Export)
                          └─► Phase 5 (Dashboards + Notifications)
                                └─► Phase 6 (Hardening + Production)
```

Phases are strictly sequential. No phase may begin until all gate conditions for the previous phase are satisfied. This is a deliberate choice to prevent accumulated technical debt from domain ambiguity.

---

## Items Not in Current Scope

The following were identified but are explicitly OUT OF SCOPE for this roadmap:

| Item | Reason |
|---|---|
| Timetable generation module | Requires institutional confirmation; likely handled externally |
| Student grievance / appeal workflow | Complex workflow; requires separate requirements analysis |
| Parent portal | Not mentioned in requirements |
| SMS gateway integration | Notification channel TBD by institution |
| Multi-institution / SaaS deployment | Not in current scope; schema must not preclude it |
| Mobile application | Not in scope; the web app must be responsive |
| Biometric / RFID attendance integration | Not in scope; system is manual/import-based |
| Learning Management System (LMS) integration | Not in scope |

---

*End of ROADMAP.md*
