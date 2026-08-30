# Attendance Management and Calculation System
# ARCHITECTURE_NOTES.md — Phase 0

**Document Status:** Draft  
**Version:** 0.1.0  
**Date:** 2026-08-29  
**Classification:** Internal — Architecture & Planning

---

## Table of Contents

1. [Guiding Principles](#1-guiding-principles)
2. [Technology Stack](#2-technology-stack)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Backend Module Decomposition](#4-backend-module-decomposition)
5. [Attendance Calculation Engine Design](#5-attendance-calculation-engine-design)
6. [Caching Strategy](#6-caching-strategy)
7. [Asynchronous Processing](#7-asynchronous-processing)
8. [Database Design Guidelines](#8-database-design-guidelines)
9. [Security Architecture](#9-security-architecture)
10. [Observability Architecture](#10-observability-architecture)
11. [CI/CD and Deployment](#11-cicd-and-deployment)
12. [Key Architectural Decisions (ADRs)](#12-key-architectural-decisions-adrs)
13. [Decisions Deferred to Phase 1+](#13-decisions-deferred-to-phase-1)

---

## 1. Guiding Principles

These principles govern every architectural decision in this system:

1. **Source of Truth is Raw Data.** Attendance percentages are always derived, never stored as user-entered values. The database holds sessions and attendance records; everything else is computed.

2. **Policy Isolation.** Attendance calculation rules are first-class domain objects, not scattered if/else conditions. A policy change MUST NOT require code changes.

3. **Auditability by Design.** Every state mutation is audited. Audit data is immutable. No domain entity is deleted without an audit trail.

4. **Correctness over Convenience.** When in doubt between a simpler implementation and a correct one, choose correctness. Attendance affects student academic standing.

5. **Testability at Every Layer.** Domain logic (policy evaluation) is pure and testable without infrastructure. Integration tests use real PostgreSQL via Testcontainers.

6. **Explicit over Implicit.** No magic behavior. All configuration is explicit and queryable. No hidden defaults that silently change behavior.

7. **Defense in Depth.** Authorization is enforced at the service layer, not only at the controller or API gateway. Database constraints are the last line of defense, not the only one.

8. **Separation of Concerns.** The API layer knows about HTTP. The service layer knows about business operations. The domain layer knows about business rules. The repository layer knows about persistence. No layer bypasses the layer above it.

---

## 2. Technology Stack

### 2.1 Frontend

| Component | Technology | Rationale |
|---|---|---|
| Framework | React 18+ | Component model ideal for complex attendance grids and forms |
| Language | TypeScript | Type safety critical for attendance data structures |
| Build Tool | Vite | Fast HMR, excellent TS support |
| Styling | Tailwind CSS | Utility-first, consistent design without custom CSS overhead |
| State Management | TBD (Zustand / React Query) | To be decided in Phase 1 based on complexity |
| HTTP Client | Axios or Fetch API | TBD in Phase 1 |
| Table Library | TBD (TanStack Table v8 candidate) | Required for complex attendance grids with sorting/filtering |

### 2.2 Backend

| Component | Technology | Rationale |
|---|---|---|
| Framework | Spring Boot 3.x | Production-grade, mature, excellent JPA/Security integration |
| Language | Java 21 | LTS, virtual threads for high concurrency via Project Loom |
| Persistence | Spring Data JPA + Hibernate | ORM for relational model; native queries where performance demands |
| Security | Spring Security 6.x | Role-based access, JWT integration, method-level security |
| Validation | Jakarta Bean Validation (Hibernate Validator) | Declarative validation at DTO layer |
| Excel | Apache POI (SXSSF/XSSF) | SXSSF (streaming) for large exports; XSSF for imports |
| API Docs | SpringDoc OpenAPI 3 | Auto-generated from annotations |
| Scheduling | Spring @Scheduled / Quartz (TBD) | For calculation jobs and notification triggers |

### 2.3 Database

| Component | Technology | Rationale |
|---|---|---|
| Primary Database | PostgreSQL 15+ | ACID, row-level security, JSONB for audit snapshots, partitioning support |
| Migration | Flyway | Versioned, repeatable migrations; audit trail for schema changes |
| Connection Pool | HikariCP | Default in Spring Boot; fast, reliable |

### 2.4 Infrastructure

| Component | Technology | Rationale |
|---|---|---|
| Containerization | Docker + Docker Compose | Reproducible local and CI environments |
| CI/CD | GitHub Actions | Integrated with repository; matrix builds for test parallelism |
| Testing | JUnit 5 + Mockito + Testcontainers | Full test pyramid coverage |
| Caching | Redis (if caching needed) | TBD; Spring Cache abstraction allows easy swap |
| Async Jobs | Spring @Async / future: dedicated job queue | TBD in Phase 2 based on load requirements |

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT TIER                          │
│   React + TypeScript + Vite + Tailwind CSS                  │
│   (Browser SPA)                                             │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS / REST + JSON
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                       API GATEWAY / REVERSE PROXY           │
│   (Nginx or similar in production)                          │
│   TLS termination, rate limiting, static asset serving      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    SPRING BOOT APPLICATION                   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  REST API    │  │  Security    │  │  OpenAPI/Swagger  │  │
│  │  Controllers │  │  Filter Chain│  │  UI               │  │
│  └──────┬───────┘  └──────────────┘  └──────────────────┘  │
│         │                                                   │
│  ┌──────▼──────────────────────────────────────────────┐   │
│  │               SERVICE LAYER                          │   │
│  │  AttendanceService | SessionService | ImportService  │   │
│  │  ReportService | UserService | AuditService          │   │
│  └──────┬──────────────────────────────────────────────┘   │
│         │                                                   │
│  ┌──────▼──────────────────────────────────────────────┐   │
│  │              DOMAIN / POLICY LAYER                   │   │
│  │  AttendanceCalculationEngine                         │   │
│  │  PolicyEvaluator | ShortageCalculator                │   │
│  │  PredictionEngine | CondonationEvaluator             │   │
│  └──────┬──────────────────────────────────────────────┘   │
│         │                                                   │
│  ┌──────▼──────────────────────────────────────────────┐   │
│  │            REPOSITORY / DATA ACCESS LAYER            │   │
│  │  Spring Data JPA Repositories + Native SQL Queries   │   │
│  └──────┬──────────────────────────────────────────────┘   │
│         │                                                   │
│  ┌──────▼──────────────────────────────────────────────┐   │
│  │         INFRASTRUCTURE / CROSS-CUTTING LAYER         │   │
│  │  AuditInterceptor | CacheManager | AsyncJobRunner     │   │
│  │  ExcelImportPipeline | ExcelExportPipeline           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────┘
                              │
            ┌─────────────────┴────────────────┐
            │                                  │
┌───────────▼───────┐              ┌───────────▼───────┐
│   PostgreSQL 15+   │              │   Redis (TBD)     │
│   Primary Database │              │   Cache / Session │
└───────────────────┘              └───────────────────┘
```

---

## 4. Backend Module Decomposition

### 4.1 Module Boundaries

The backend SHALL be organized as a modular monolith initially. Each module has a well-defined public API (service interfaces) and internal implementation. Modules communicate only via service interfaces, never via direct repository access across module boundaries.

```
com.amcs
├── config/                  # Global config: security, JPA, async, cache
├── common/                  # Shared DTOs, exceptions, pagination, audit base
│
├── academic/                # MODULE: Academic Structure
│   ├── domain/              #   Entities: Institution, AcademicYear, Department,
│   │                        #   Programme, Section, StudentEnrollment
│   ├── repository/
│   ├── service/
│   └── api/                 #   REST controllers for academic structure
│
├── user/                    # MODULE: User and Authentication
│   ├── domain/              #   Entities: User, Role, Permission
│   ├── repository/
│   ├── service/
│   └── api/                 #   REST controllers: auth, user management
│
├── subject/                 # MODULE: Subjects and Faculty Assignment
│   ├── domain/              #   Entities: Subject, SubjectSection, FacultyAssignment
│   ├── repository/
│   ├── service/
│   └── api/
│
├── session/                 # MODULE: Session Management
│   ├── domain/              #   Entities: Session, SessionStatus
│   ├── repository/
│   ├── service/
│   └── api/
│
├── attendance/              # MODULE: Attendance Recording and Calculation (CORE)
│   ├── domain/
│   │   ├── record/          #   AttendanceRecord entity
│   │   ├── policy/          #   AttendancePolicy, OverallAttendancePolicy, CondonationRule
│   │   └── calculation/     #   AttendanceCalculationEngine, PolicyEvaluator,
│   │                        #   ShortageCalculator, PredictionEngine
│   ├── repository/
│   ├── service/
│   └── api/
│
├── importexport/            # MODULE: Excel Import and Export
│   ├── importer/            #   ImportPipeline, RowValidator, ImportJobRepository
│   ├── exporter/            #   ExportService, ReportBuilders
│   └── api/
│
├── report/                  # MODULE: Reporting
│   ├── service/             #   ReportService (composes attendance + academic modules)
│   └── api/
│
├── audit/                   # MODULE: Audit Logging (cross-cutting)
│   ├── domain/              #   AuditLog entity
│   ├── repository/
│   ├── service/
│   └── api/                 #   Audit log query endpoint
│
└── notification/            # MODULE: Notifications (optional, TBD)
    └── service/
```

### 4.2 Cross-Cutting Concerns

| Concern | Implementation |
|---|---|
| Audit Logging | Spring AOP aspect on all @Service methods that mutate state |
| Correlation ID | Servlet filter that sets MDC correlation ID from header or generates one |
| Exception Handling | @ControllerAdvice with structured error response format |
| Validation | Jakarta Bean Validation on all DTOs; custom validators for domain rules |
| Transaction Management | @Transactional at service layer; read-only transactions for queries |
| Caching | Spring Cache abstraction; cache keys include policy version |

---

## 5. Attendance Calculation Engine Design

This is the most critical module in the system. Its design must be correct, testable, and extensible.

### 5.1 Core Design Pattern: Strategy + Policy Object

```
AttendanceCalculationEngine
  ├── resolvePolicy(subject) -> AttendancePolicy
  ├── computeSubjectAttendance(student, subject, period) -> SubjectAttendanceResult
  └── computeOverallAttendance(student, period) -> OverallAttendanceResult

AttendancePolicy (immutable value object)
  ├── id: UUID
  ├── name: String
  ├── theoryUnitWeight: int          // units counted per theory session
  ├── labUnitWeight: int             // units counted per lab session
  ├── statusContributions: Map<AttendanceStatus, Fraction>
  │   // e.g., PRESENT -> 1.0, DUTY_LEAVE -> 1.0, MEDICAL_LEAVE -> 0.5, ABSENT -> 0.0
  ├── minimumThresholdPercentage: BigDecimal
  ├── condonationPolicy: CondonationPolicy (nullable)
  └── version: int                  // for cache invalidation

SubjectAttendanceResult (value object)
  ├── studentId
  ├── subjectId
  ├── period
  ├── policyId (+ policyVersion)
  ├── conductedUnits: int
  ├── attendedUnits: BigDecimal      // BigDecimal to handle fractional weights
  ├── percentage: BigDecimal
  ├── isShortage: boolean
  ├── unitsShort: BigDecimal
  ├── condonationApplied: BigDecimal
  └── computedAt: Instant

OverallAttendancePolicy (immutable value object)
  ├── aggregationStrategy: AggregationStrategy enum
  │   // ARITHMETIC_MEAN, WEIGHTED_BY_CREDITS, AGGREGATE_HOURS, CUSTOM
  ├── subjectWeightResolver: (for WEIGHTED_BY_CREDITS strategy)
  └── minimumThresholdPercentage: BigDecimal

PredictionEngine
  ├── computeRequiredFutureClasses(result, remainingSessions) -> int
  └── computeMaxAllowedAbsences(result, remainingSessions) -> int
```

### 5.2 Calculation Algorithm (Subject-Level)

The algorithm MUST NOT be inlined anywhere except the domain layer:

```
computeSubjectAttendance(student, subject, period):
  1. Load all CONDUCTED sessions for subject+section within period.
  2. For each session, resolve its sessionType (THEORY or LAB).
  3. For each session, determine conductedUnits (from Session.conductedUnits field).
  4. Compute denominator = sum of (session.conductedUnits * policy.unitWeight(session.sessionType))
     for all sessions in student's enrollment window.
  5. For each session, load student's AttendanceRecord.
     - If record is missing and session is CONDUCTED: treat as ABSENT (log warning).
  6. For each record, resolve contribution fraction from policy.statusContributions.
  7. Compute numerator = sum of (session.conductedUnits * policy.unitWeight(session.type)
                                  * policy.statusContribution(record.status))
  8. Apply condonation if CondonationPolicy is present:
     - Method TBD: REQUIRES INSTITUTIONAL CONFIRMATION.
  9. percentage = (numerator / denominator) * 100, rounded to configured decimal places.
  10. isShortage = percentage < policy.minimumThresholdPercentage.
  11. Return SubjectAttendanceResult (immutable).
```

### 5.3 Important Design Constraints

- **BigDecimal throughout.** Never use float or double for attendance calculations. Use BigDecimal with HALF_UP rounding.
- **No side effects.** The engine never writes to the database. It only reads and computes.
- **Policy snapshot.** The policy used for computation is captured in the result. Enables reproducibility.
- **Enrollment window enforcement.** Sessions before a student's enrollment start date or after their enrollment end date (for that section) are excluded from their denominator AND numerator.

---

## 6. Caching Strategy

### 6.1 What to Cache

| Data | Cache Strategy | Invalidation Trigger |
|---|---|---|
| Active attendance policies | In-memory (Spring Cache + Caffeine) | Policy version change |
| Academic structure (departments, sections) | In-memory with TTL 5 min | Any structure change |
| Subject attendance results | Conditional (if computation is expensive) | New session marked, attendance record changed, policy changed, session status changed |
| Dashboard aggregates | Short TTL (2-5 min) | Any underlying data change |

### 6.2 Caching Cautions

- **Do NOT cache attendance percentages across policy version changes.** Cache keys MUST include policyVersion.
- **Do NOT trust cached results for official report generation.** Reports MUST recompute from source.
- **Do NOT cache audit logs.** Always read from the database.

---

## 7. Asynchronous Processing

### 7.1 Use Cases Requiring Async

| Use Case | Trigger | Queue / Mechanism |
|---|---|---|
| Large XLSX export | User requests export of > 1000 rows | Spring @Async / Future: dedicated queue |
| Bulk attendance import | File upload | Spring @Async |
| Attendance shortage notification | Threshold breached | Event listener (Spring ApplicationEvent) |
| Scheduled calculation refresh | Configurable cron | Spring @Scheduled |
| Audit log archival | Nightly cron | Spring @Scheduled |

### 7.2 Job Status Tracking

Every async job MUST be persisted in a `JobRecord` table with:
- jobId, jobType, status (PENDING / RUNNING / COMPLETED / FAILED)
- submittedBy, submittedAt, startedAt, completedAt
- resultLocation (for export download URL)
- errorMessage (if failed)

---

## 8. Database Design Guidelines

### 8.1 Schema Strategy

- **Separate schema per major domain.** Consider schemas: `academic`, `attendance`, `audit`, `users`.
- **UUID primary keys.** All entities use UUID (type 4) as primary keys to avoid sequential ID exposure and support future distribution.
- **Soft deletes for core entities.** Entities (students, subjects, sections) use `deleted_at` timestamp instead of physical DELETE to preserve referential integrity.
- **Temporal modeling for enrollment.** `student_enrollment` has `enrollment_start` and `enrollment_end` date columns. A NULL `enrollment_end` means currently active.

### 8.2 Critical Unique Constraints

```sql
-- Prevent duplicate attendance records
UNIQUE (session_id, student_id) ON attendance_record

-- Prevent duplicate sessions
UNIQUE (subject_id, section_id, session_date, start_time) ON session

-- Prevent duplicate enrollments
UNIQUE (student_id, section_id, enrollment_start) ON student_enrollment
```

### 8.3 Indexing Strategy

| Table | Index | Rationale |
|---|---|---|
| attendance_record | (session_id, student_id) | FK lookup + unique constraint |
| attendance_record | (student_id, session_id) | Student-centric queries |
| session | (subject_id, section_id, session_date) | Date-range queries for calculation |
| session | (session_date, status) | Dashboard queries for conducted sessions |
| audit_log | (entity_type, entity_id) | Audit query by entity |
| audit_log | (actor_id, created_at) | Audit query by actor |
| student_enrollment | (student_id, enrollment_start, enrollment_end) | Enrollment window queries |

### 8.4 Audit Table Design

The `audit_log` table MUST be write-only from the application. Consider using PostgreSQL Row-Level Security to prevent UPDATE/DELETE even from the application user. The application user for the main schema MUST have INSERT but NOT UPDATE/DELETE on `audit_log`.

### 8.5 Partitioning Consideration

If audit_log grows large, consider range partitioning by `created_at` year. This is a Phase 3+ concern but the table design must not preclude it.

---

## 9. Security Architecture

### 9.1 Authentication Flow

```
Client --> POST /api/auth/login (username + password)
  --> Spring Security AuthenticationManager
  --> UserDetailsService (loads user + roles from DB)
  --> BCryptPasswordEncoder.matches()
  --> If success: issue Access Token (JWT, 15 min) + Refresh Token (opaque, stored in DB)
  --> Client stores Access Token in memory (NOT localStorage)
  --> Client stores Refresh Token in httpOnly secure cookie

Client --> Authenticated request (Authorization: Bearer <access_token>)
  --> JwtAuthenticationFilter
  --> Validates signature + expiry + issuer
  --> Sets SecurityContext

Client --> POST /api/auth/refresh (Refresh Token from cookie)
  --> Validate Refresh Token against DB (stored hash)
  --> Rotate: issue new Refresh Token, invalidate old one
  --> Issue new Access Token
```

### 9.2 Authorization Filter Chain

```
@PreAuthorize("hasRole('FACULTY')")          // Controller layer (supplementary)
      +
@Service method security                     // Service layer (primary enforcement)
      +
Repository query filters (tenant/section scope)  // Data layer (last resort)
```

### 9.3 Data Scoping

- **Student queries:** `WHERE student_id = :currentUserId` enforced in all student-facing service methods. Never trust a student-supplied ID from request parameters for sensitive data.
- **Faculty queries:** `WHERE faculty_id = :currentUserId AND session_id IN (SELECT id FROM session WHERE faculty_id = :currentUserId)` for attendance marking.
- **Department Admin queries:** `WHERE department_id = :userDepartmentId` for all department-scoped operations.

---

## 10. Observability Architecture

### 10.1 Logging

- **Format:** JSON structured logs (via Logback with logstash-logback-encoder).
- **Fields:** timestamp, level, logger, message, correlationId, userId, requestPath, durationMs.
- **Levels:** ERROR for exceptions, WARN for anomalies (late attendance marking, missing records), INFO for audit-worthy events, DEBUG for diagnostic.
- **No PII in logs:** Never log student names, roll numbers, or attendance details at DEBUG level in production.

### 10.2 Metrics

Expose via Spring Actuator + Micrometer:
- `attendance.calculation.duration` (timer, tagged by calculation type)
- `import.job.duration` (timer, tagged by import type)
- `export.job.duration` (timer)
- `attendance.records.marked.total` (counter)
- `sessions.conducted.total` (counter)
- `http.server.requests` (default Micrometer metric)

### 10.3 Health Checks

- `/actuator/health` — liveness + readiness probe
- `/actuator/health/db` — PostgreSQL connectivity
- `/actuator/health/redis` — Redis connectivity (if applicable)
- `/actuator/info` — build info, version, git commit

---

## 11. CI/CD and Deployment

### 11.1 GitHub Actions Pipeline

```
on: push / pull_request
  ├── Lint (Checkstyle for Java, ESLint/Prettier for TypeScript)
  ├── Unit Tests (JUnit 5 + Mockito, no DB required)
  ├── Integration Tests (JUnit 5 + Testcontainers with PostgreSQL)
  ├── Build (Maven / Gradle + Vite build)
  ├── Docker Image Build (multi-stage Dockerfile)
  └── (on main branch) Deploy to staging
```

### 11.2 Docker Compose Topology (Local / CI)

```yaml
services:
  db:         # PostgreSQL 15
  redis:      # Redis (if caching in scope)
  backend:    # Spring Boot (depends_on: db)
  frontend:   # Nginx serving Vite build (depends_on: backend)
```

### 11.3 Multi-Stage Docker Build

Backend Dockerfile:
- Stage 1: Maven/Gradle build (JDK 21)
- Stage 2: Runtime (JRE 21 slim)
- Run as non-root user
- No secrets in image layers

Frontend Dockerfile:
- Stage 1: Node build (node:20-alpine)
- Stage 2: Nginx serving static assets

---

## 12. Key Architectural Decisions (ADRs)

### ADR-001: Modular Monolith over Microservices

**Decision:** Start as a modular monolith.  
**Rationale:** A university attendance system of this scale does not warrant microservices complexity. A modular monolith with clean module boundaries provides the same extensibility at a fraction of the operational overhead. The module boundaries are designed to allow future extraction if needed.  
**Consequences:** No inter-service network calls, no distributed transactions. Single deployment unit. Higher risk of module boundary violations requires discipline via package visibility rules and architecture tests (ArchUnit).

### ADR-002: Computed Attendance, Not Stored Percentages

**Decision:** Attendance percentages are always computed from raw session and attendance records.  
**Rationale:** Storing percentages creates a dual-write problem, makes auditing impossible, and violates the single source of truth principle.  
**Consequences:** Computation must be fast. Caching strategy becomes important. A change to a session or attendance record must invalidate related caches.

### ADR-003: Policy as a Domain Entity, Not Configuration

**Decision:** Attendance calculation policies are stored in the database as first-class entities with version history, not as application.properties values.  
**Rationale:** Policies can differ by department, by subject type, and over time. Application properties cannot represent this. Database-stored policies are queryable, auditable, and can be applied retroactively with full transparency.

### ADR-004: BigDecimal for All Attendance Math

**Decision:** All attendance numerator, denominator, and percentage values use `java.math.BigDecimal`.  
**Rationale:** Float/double arithmetic produces rounding errors that are unacceptable in an academic context where a student's standing depends on exact percentage values.

### ADR-005: UUID Primary Keys

**Decision:** All entities use UUID v4 as primary keys.  
**Rationale:** Prevents sequential ID enumeration attacks, supports future data federation or migration, and decouples ID generation from the database.

### ADR-006: Flyway for Schema Management

**Decision:** Flyway for all database migrations.  
**Rationale:** Versioned, repeatable, and checked-in migrations provide a full history of schema evolution. Liquibase is an alternative but Flyway's simplicity is preferred for this system size.

---

## 13. Decisions Deferred to Phase 1+

| Decision | Reason for Deferral |
|---|---|
| Redis vs. in-memory cache | Load testing needed to determine if caching is necessary |
| Dedicated job queue (RabbitMQ/SQS) vs. Spring @Async | Async job complexity depends on notification scope decision |
| Frontend state management library | Depends on final component design |
| Timetable module in-scope vs. external | Requires institutional confirmation |
| Notification delivery channel (email/SMS/push) | Requires institutional confirmation |
| Read replica for reporting queries | Depends on query load profiling |
| Audit log partitioning | Phase 3+ concern |
| Multi-tenancy (multiple institutions) | Not in current scope, but schema should not preclude it |

---

*End of ARCHITECTURE_NOTES.md*
