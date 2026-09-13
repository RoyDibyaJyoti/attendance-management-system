# AMCS Technical Architecture Guide

This document provides a comprehensive technical breakdown of the **Attendance Management and Calculation System (AMCS)** for software engineers, technical interviewers, and system architects.

---

## 1. System Overview

AMCS is a production-grade academic attendance and compliance system designed to replace spreadsheet-based records with an auditable, concurrent, and high-throughput application. The platform handles:

- **Complex calculation models:** Component-weighted attendance (theory vs. laboratory), multi-subject aggregations, and predictive shortage projections.
- **Academic realities:** Mid-term section transfers, temporal enrollment windowing, session rescheduling, and audited post-conducted attendance corrections.
- **Enterprise constraints:** Stateless JWT authentication with instant revocation, service-layer IDOR protection, optimistic concurrency control, and streaming memory-safe spreadsheet reporting.

The system is architected as a **Hexagonal Architecture (Ports and Adapters)** monolith with a clean separation of concerns, zero-dependency domain logic, and a containerized deployment footprint.

---

## 2. Component Architecture

```
+-----------------------------------------------------------------------------+
|                               Client Tier                                   |
|   React 19 + TypeScript Single Page Application (SPA)                       |
+-----------------------------------------------------------------------------+
                                     |
                                     | HTTPS / HTTP Port 80
                                     v
+-----------------------------------------------------------------------------+
|                           Ingress & Reverse Proxy                           |
|   Nginx 1.27 Alpine                                                         |
|   - Serves cached static assets (/assets/*) with gzip compression           |
|   - SPA client-side fallback (try_files $uri $uri/ /index.html)             |
|   - Reverse proxy for /api/* and /actuator/health                           |
|   - Anti-spoofing client IP propagation (X-Real-IP $remote_addr)            |
+-----------------------------------------------------------------------------+
                                     |
                                     | Internal Docker Network (amcs-prod-network)
                                     v
+-----------------------------------------------------------------------------+
|                          Spring Boot 3.3.3 Backend                          |
|                                                                             |
|  [Perimeter Filters]                                                        |
|   - RateLimitingFilter (Bucket4j token-bucket algorithm)                    |
|   - JwtAuthenticationFilter (HMAC-SHA256 signature & tokenVersion lookup)   |
|                                                                             |
|  [Web Layer / Adapters]                                                     |
|   - Spring MVC REST Controllers                                             |
|   - Inbound DTO Mappings & Jakarta Validation (@Valid)                      |
|                                                                             |
|  [Application Layer]                                                        |
|   - Application Services (Transactions, Workflows, Orchestration)           |
|   - ApplicationAuthorizationService (Role checking & IDOR ownership)        |
|   - Input Ports (Use Case Interfaces) & Output Ports (Repository Contracts) |
|                                                                             |
|  [Domain Core - Pure Java 21]                                               |
|   - AttendanceCalculationEngine, ShortageCalculator, PredictionEngine       |
|   - Immutable Aggregates & Records: AttendanceRecord, Session, Policy       |
|   - Business Rule Invariants & Domain Exceptions                            |
|                                                                             |
|  [Infrastructure Adapters]                                                  |
|   - Persistence Adapters (Spring Data JPA / Hibernate 6.5)                  |
|   - Streaming Excel Adapter (Apache POI SXSSF & StAX XML Readers)           |
+-----------------------------------------------------------------------------+
                                     |
                                     | JDBC Connection Pool (HikariCP)
                                     v
+-----------------------------------------------------------------------------+
|                               Database Tier                                 |
|   PostgreSQL 16 Alpine                                                      |
|   - Schema managed strictly by Flyway Community 10.x                        |
|   - Relational tables with foreign key constraints & unique indexes         |
|   - Persistent named volume (amcs_pgdata)                                   |
+-----------------------------------------------------------------------------+
```

---

## 3. Backend Package Architecture

AMCS follows a strict Hexagonal (Clean Architecture) package organization:

```
com.amcs
├── domain                     <-- PURE JAVA CORE (Zero framework dependencies)
│   ├── calculation            # Mathematical calculation engine, shortage forecaster
│   ├── model                  # Domain entities, records, and aggregates (Session, Record, Policy)
│   ├── policy                 # Versioned attendance policies, strategy interfaces
│   ├── exception              # Pure domain business rule exceptions
│   └── valueobject            # Value objects (DateRange, AttendancePercentage, SessionStatus)
│
├── application                <-- USE CASES & WORKFLOWS
│   ├── port
│   │   ├── in                 # Inbound use case interfaces (e.g., RecordAttendanceUseCase)
│   │   └── out                # Outbound repository and reporting contracts
│   ├── service                # Orchestration services implementing inbound ports
│   ├── dto                    # Inbound commands and outbound API response records
│   ├── security               # ApplicationAuthorizationService (IDOR enforcement)
│   └── exception              # Application-layer exceptions (NotFound, Conflict, Forbidden)
│
└── infrastructure             <-- FRAMEWORK & ADAPTERS
    ├── web
    │   ├── controller         # Spring MVC REST Controllers (@RestController)
    │   ├── security           # Spring Security configuration, JWT filter, TokenProvider
    │   └── exception          # GlobalExceptionHandler (@ControllerAdvice)
    ├── persistence
    │   ├── entity             # JPA Entities (@Entity, @Table, @Version)
    │   ├── repository         # Spring Data JPA Repository interfaces
    │   └── adapter            # Outbound port implementations mapping JPA -> Domain
    └── excel
        ├── exporter           # Apache POI SXSSFWorkbook streaming report generators
        └── importer           # StAX memory-safe Excel ingest parsers
```

### Dependency Inversion Rule
The `domain` package never imports from `application` or `infrastructure`. The `application` package depends only on `domain`. The `infrastructure` package depends on `application` and `domain`. Persistence and web technologies can be replaced without modifying a single line of calculation or policy logic.

---

## 4. Authentication & Authorization Flow

```
1. Client POST /api/v1/auth/login (username, password)
   │
2. AuthenticationController -> AuthenticationApplicationService
   │
3. PasswordEncoder (BCrypt-12) verifies hash against user_credentials table
   │
4. TokenProvider generates signed HMAC-SHA256 JWT containing:
   - sub: username
   - role: STUDENT / FACULTY / HOD_ADMIN
   - tokenVersion: current user token_version integer
   - iat / exp: validity timestamp
   │
5. Subsequent Request:
   Client sends header "Authorization: Bearer <jwt>"
   │
6. JwtAuthenticationFilter:
   a. Validates signature & expiry
   b. Extracts username and tokenVersion claim
   c. Queries UserRepositoryPort for active token_version
   d. If claim version != DB version -> 401 Unauthorized (INSTANT REVOCATION)
   e. Populates Spring SecurityContextHolder with UserPrincipal
   │
7. Spring Security Filter Chain:
   Verifies role permissions for endpoint path (e.g. /api/v1/admin/** -> HOD_ADMIN)
   │
8. ApplicationAuthorizationService:
   Verifies fine-grained context ownership (IDOR Defense):
   - If STUDENT: studentId must match authenticated user's linked studentId
   - If FACULTY: facultyId must match assigned teacher for session's section/subject
   - If HOD_ADMIN: unrestricted institutional access
```

---

## 5. Attendance Calculation Flow

The attendance calculation engine (`AttendanceCalculationEngine`) processes raw records into mathematically sound eligibility percentages:

### Conducted vs. Attended Units
Sessions have a `unitCount` (typically `1.0` for 1-hour Theory, `2.0` for 2-hour Laboratory).
- **Conducted Units:** Sum of `unitCount` for all sessions of the subject conducted within the student's enrollment window.
- **Attended Units:**
  - `PRESENT`: Contributes `1.0 * unitCount`.
  - `DUTY_LEAVE`: Treated as attended (`1.0 * unitCount`) per standard university policy.
  - `MEDICAL_LEAVE`: Configurable by policy (e.g., treated as excused or attended within percentage caps).
  - `ABSENT`: Contributes `0.0`.

### Attendance Percentage Formula
$$\text{Attendance \%} = \left( \frac{\text{Total Attended Units}}{\text{Total Conducted Units}} \right) \times 100$$
Calculated with `BigDecimal` arithmetic using `RoundingMode.HALF_UP` to prevent floating-point precision leakage.

### Shortage and Predictive Forecaster
Given a policy threshold $T$ (e.g., 75%), currently attended units $A$, currently conducted units $C$, and remaining planned units $R$:
- If current percentage $< T$, the student is in **Shortage**.
- The minimum consecutive future sessions $X$ required to achieve eligibility is computed via:
$$\frac{A + X}{C + X} \ge T \implies X \ge \frac{T \cdot C - A}{1 - T}$$
- If $X > R$, recovery is mathematically impossible for the semester.

---

## 6. Temporal Enrollment and Section Transfer Model

In traditional systems, transferring a student from Section A to Section B either deletes their Section A attendance or attributes Section B's earlier sessions to them. AMCS models enrollments as temporal intervals:

```
Section A: [2026-08-01 ─────── 2026-09-15] (Status: TRANSFERRED)
                                   │
                                   ▼ Transfer Event
Section B:                    [2026-09-16 ─────── 2026-12-20] (Status: ACTIVE)
```

When calculating attendance:
1. Every session has an immutable `sessionDate`.
2. The engine filters sessions to only those falling within $[ \text{enrollment.startDate}, \text{enrollment.endDate} ]$.
3. Sessions conducted in Section A before Sept 16 are credited to Section A records; sessions conducted in Section B on or after Sept 16 are credited to Section B.

---

## 7. Attendance Correction Model

Attendance records are auditable legal records. AMCS prohibits silent mutations:

1. **Initial Recording:** A scheduled session is transitioned to `CONDUCTED` during roll call, generating initial `attendance_records` entries.
2. **Post-Conduct Modification:** Once conducted, records cannot be overwritten via standard roll call.
3. **Correction Workflow:**
   - Faculty or Admin submits an `AttendanceCorrectionRequest` specifying `recordId`, `newStatus`, `reason`, and `approverId`.
   - The system validates that the reason is non-empty and that the session has not been locked by semester closing.
   - An immutable entry is appended to `attendance_corrections` storing the original status, new status, timestamp, reason, and actor ID.
   - The live record status is updated atomically in the same database transaction.

---

## 8. Optimistic Concurrency Control

In high-concurrency environments, multiple faculty members or teaching assistants may access the roll-call page for a multi-instructor laboratory session simultaneously.

To prevent the **Lost Update Problem**, the `SessionEntity` incorporates a JPA `@Version` column:
```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```
When staff member 1 saves roll call, the version increments from `1` to `2`. When staff member 2 attempts to submit attendance using stale state (version `1`), Hibernate throws an `OptimisticLockException`. The application catches this and returns an HTTP `409 Conflict` prompting the user to refresh the latest state rather than overwriting a colleague's records.

---

## 9. Reporting Architecture

AMCS supports 8 institutional Excel reports (RPT-001 through RPT-008). 

### Memory Safety via Apache POI SXSSF
Traditional DOM-based Excel libraries (`XSSFWorkbook`) load the entire spreadsheet XML into JVM heap, causing `OutOfMemoryError` on large student rosters.
- AMCS utilizes `SXSSFWorkbook` (Streaming POI) with a sliding window of 100 rows kept in memory.
- Completed rows are flushed to temporary disk files, maintaining a constant $O(1)$ memory consumption regardless of whether generating 50 rows or 50,000 rows.
- Response streams binary data directly into the HTTP servlet output stream with media type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

---

## 10. Spreadsheet Ingestion Security

Bulk Excel roster uploads introduce severe security vulnerabilities if not guarded:

1. **Decompression Bomb (ZIP Bomb) Defense:**
   - Excel `.xlsx` files are compressed ZIP archives.
   - The ingest pipeline inspects compression headers and enforces a maximum expansion ratio of $20.0$. Files exceeding this ratio are rejected before decompression.
2. **XML External Entity (XXE) Prevention:**
   - Underlying StAX XML readers are explicitly configured with `XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES = false` and `SUPPORT_DTD = false`, thwarting server-side file disclosure and SSRF attacks.
3. **CSV / Excel Formula Injection (CWE-1236):**
   - User-supplied strings (student names, roll numbers) starting with `=`, `+`, `-`, or `@` can trigger malicious command execution when opened in Microsoft Excel.
   - All string exports sanitize these characters by prepending a single apostrophe (`'`).

---

## 11. Deployment Architecture

AMCS deploys as a Docker Compose multi-container stack:

```
[ amcs-frontend (Nginx 1.27) ] : Port 80
             │
             │ internal proxy: http://amcs-backend:8080
             ▼
[ amcs-backend (Java 21 JRE) ] : Port 8080 (Non-root UID 10001)
             │
             │ internal jdbc: postgresql://amcs-postgres:5432/amcs_db
             ▼
[ amcs-postgres (Postgres 16) ] : Port 5432 (Persistent volume: amcs_pgdata)
```

### Health Dependency Chaining
- `amcs-postgres` exposes a native `pg_isready` health check.
- `amcs-backend` waits for PostgreSQL to report `service_healthy` before executing Flyway migrations and starting Spring Boot.
- `amcs-frontend` waits for `amcs-backend` to report `service_healthy` via Spring Actuator before accepting ingress traffic.

---

## 12. Future Scaling Considerations

While the current architecture is tuned for single-instance or moderately sized institutional deployments, scaling to hundreds of thousands of students across multiple campus branches would involve:

1. **Distributed Rate Limiting:**
   - *Current:* Single-node JVM in-memory Token Bucket (`Bucket4j` / `ConcurrentHashMap`).
   - *Scaling Path:* Introduce Redis-backed token buckets (e.g. `Bucket4j-Redis`) or enforce rate limiting at an external API Gateway / Cloudflare edge.
2. **Read/Write Splitting & Replica Pools:**
   - Read-heavy calculation queries and reporting jobs can be routed to read-only PostgreSQL replicas using Spring Data routing data sources (`AbstractRoutingDataSource`).
3. **Asynchronous Report Queuing:**
   - For campus-wide multi-semester reports exceeding 100,000 students, decouple generation into asynchronous background jobs utilizing RabbitMQ/Kafka workers and S3 object storage delivery.
4. **Centralized Log Aggregation:**
   - Ship structured JSON logs via fluentbit/promtail to Grafana Loki or Datadog for distributed tracing and centralized alerting.
