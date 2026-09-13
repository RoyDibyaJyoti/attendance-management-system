# AMCS Backend Technical Interview Guide

This guide is designed for preparing to defend the **Attendance Management and Calculation System (AMCS)** in senior Java/Spring Boot backend engineering interviews and system design discussions.

Each question is structured with:
1. **Concise Answer** (30-second summary for the interviewer)
2. **Deep Dive** (Technical details, trade-offs, and design rationale)
3. **Where in AMCS** (Exact code artifacts, classes, and packages)

---

## 1. System Architecture

### Q1: Why did you choose Hexagonal Architecture (Ports and Adapters) over a traditional 3-tier layered architecture?
- **Concise Answer:** Hexagonal architecture enforces complete decoupling of the core calculation and business rules from frameworks, databases, and HTTP APIs, ensuring mathematical invariants can be tested in isolation without starting Spring or mocking JPA repositories.
- **Deep Dive:** In a traditional 3-tier architecture (Controller $\rightarrow$ Service $\rightarrow$ DAO), the domain model often becomes an anemic set of JPA entities littered with Hibernate annotations. If you want to change the database or test business calculations, you are forced to deal with persistence context state and lazy loading issues. Hexagonal architecture inverts this dependency: the core domain (`com.amcs.domain.*`) has **zero external dependencies** and consists of pure Java records. Outbound persistence is defined by application ports (`AcademicPeriodRepositoryPort`), which are implemented by adapters in infrastructure (`AcademicPeriodPersistenceAdapter`).
- **Where in AMCS:** 
  - Pure domain: `com.amcs.domain.calculation.AttendanceCalculationEngine`
  - Ports: `com.amcs.application.port.out.SessionRepositoryPort`
  - Adapters: `com.amcs.infrastructure.persistence.adapter.SessionPersistenceAdapter`

### Q2: How does Dependency Inversion work in practice in this project?
- **Concise Answer:** High-level modules (application services) do not import or depend on low-level modules (JPA repositories). Both depend on abstractions (interfaces/ports) declared inside the application layer.
- **Deep Dive:** Instead of `AttendanceCalculationApplicationService` injecting `SessionJpaRepository` directly, it injects the outbound port `SessionRepositoryPort`. The Spring Data repository interface resides in `infrastructure.persistence.repository`. The adapter class `SessionPersistenceAdapter` implements the application port and translates between JPA entities (`SessionEntity`) and domain records (`Session`). This allows swapping PostgreSQL for DynamoDB or an in-memory test store without touching application service code.
- **Where in AMCS:**
  - Inbound port: `com.amcs.application.port.in.CalculateAttendanceUseCase`
  - Outbound port: `com.amcs.application.port.out.AttendanceRecordRepositoryPort`
  - Adapter implementation: `com.amcs.infrastructure.persistence.adapter.AttendanceRecordPersistenceAdapter`

### Q3: Why is the Domain Core implemented with zero framework dependencies?
- **Concise Answer:** It guarantees longevity, high execution speed of tests (running 500+ unit tests in under 2 seconds), and prevents framework leaks (such as accidental Jackson mutations or Hibernate dirty checking side-effects).
- **Deep Dive:** Java 21 features like immutable records, pattern matching, and sealed interfaces allow modeling business domains cleanly without Lombok or Spring annotations. If Spring Boot or Hibernate releases a breaking major version upgrade, the mission-critical attendance algorithms remain completely unaffected and verified.
- **Where in AMCS:** `com.amcs.domain.*` (Check `pom.xml`: domain classes require no Spring or Jakarta imports).

---

## 2. Spring Boot & Web Layer

### Q4: Why Spring Boot 3.3.3 and Java 21?
- **Concise Answer:** Java 21 LTS brings modern language ergonomics (records, pattern matching, Sequenced Collections) and improved garbage collection (G1GC improvements). Spring Boot 3.3.3 brings native Jakarta EE 10 support, enhanced Spring Security 6, and production-ready Actuator metrics.
- **Deep Dive:** We leverage Java 21 records extensively for immutable DTOs, domain models, and calculation results, eliminating boilerplate while guaranteeing thread-safety. Spring Boot 3 provides out-of-the-box support for graceful shutdowns, Docker health checks, and modern declarative HTTP clients.
- **Where in AMCS:**
  - `pom.xml` (`<java.version>21</java.version>`, `<spring-boot.version>3.3.3</spring-boot.version>`)
  - Domain records: `com.amcs.domain.model.AttendanceRecord`

### Q5: How are exceptions handled uniformly across the REST API?
- **Concise Answer:** Using a centralized `@RestControllerAdvice` class that intercepts domain and application exceptions and translates them into structured RFC 7807 `ProblemDetail` or consistent JSON error payloads with precise HTTP status codes.
- **Deep Dive:** Instead of returning generic 500 errors or leaking stack traces, `GlobalExceptionHandler` handles specific exceptions:
  - `EntityNotFoundException` $\rightarrow$ 404 Not Found
  - `OptimisticLockException` $\rightarrow$ 409 Conflict
  - `AccessDeniedException` $\rightarrow$ 403 Forbidden
  - `IllegalArgumentException` / `MethodArgumentNotValidException` $\rightarrow$ 400 Bad Request with field error mappings.
- **Where in AMCS:** `com.amcs.infrastructure.web.exception.GlobalExceptionHandler`

### Q6: How do you handle input validation before requests reach application services?
- **Concise Answer:** Inbound DTO records are annotated with Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Email`) and validated at controller boundaries using `@Valid`.
- **Deep Dive:** Controllers enforce syntactic validity (e.g. valid UUID format, non-empty fields) using `@Valid`. Semantic/business validity (e.g. student is actively enrolled on the session date, session does not overlap with existing schedule) is enforced inside application services using domain checks.
- **Where in AMCS:**
  - DTO validation: `com.amcs.application.dto.attendance.RecordAttendanceRequest`
  - Controller: `com.amcs.infrastructure.web.controller.AttendanceRecordingController`

---

## 3. Security & Authorization

### Q7: How does JWT authentication work in AMCS?
- **Concise Answer:** Stateless JWTs signed with HMAC-SHA256 containing the user's ID, role, and a database-backed `tokenVersion`. Each request passes through `JwtAuthenticationFilter`, which verifies the signature and validates that the token's version matches the user's current version in the database.
- **Deep Dive:** A well-known weakness of JWTs is difficulty in immediate revocation prior to expiration. AMCS solves this by storing an integer `tokenVersion` on each `UserCredentialEntity`. When an authenticated request arrives:
  1. Signature and expiration are verified cryptographically.
  2. `tokenVersion` is read from claims and checked against the database.
  3. If the user changed their password or was logged out administratively, the database `tokenVersion` increments, instantly invalidating all previously issued tokens without storing stateful sessions.
- **Where in AMCS:**
  - Filter: `com.amcs.infrastructure.web.security.jwt.JwtAuthenticationFilter`
  - Token generator/parser: `com.amcs.infrastructure.web.security.jwt.JwtTokenProvider`
  - Entity: `com.amcs.infrastructure.persistence.entity.UserCredentialEntity`

### Q8: How did you prevent Insecure Direct Object References (IDOR)?
- **Concise Answer:** Role-based URL filters are augmented with programmatic service-level ownership checks via `ApplicationAuthorizationService`.
- **Deep Dive:** A student could have the `ROLE_STUDENT` authority, but they should never be able to query `/api/v1/students/{otherStudentId}/attendance/summary`. Spring Security's `@PreAuthorize` alone is often insufficient for dynamic entity ownership. `ApplicationAuthorizationService.assertCanAccessStudent()` verifies that the authenticated user principal either has `ROLE_HOD_ADMIN` or has a linked `studentId` matching the URL path parameter.
- **Where in AMCS:** `com.amcs.application.security.ApplicationAuthorizationService`

### Q9: Why is trusting client `X-Forwarded-For` dangerous, and how did you resolve it?
- **Concise Answer:** Malicious clients can send arbitrary `X-Forwarded-For` headers with spoofed IP addresses to bypass rate limiting. AMCS configures Nginx to overwrite `X-Real-IP` with `$remote_addr`, and the backend filter inspects `X-Real-IP` first.
- **Deep Dive:** If an application blindly reads `request.getHeader("X-Forwarded-For")`, an attacker can rotate `X-Forwarded-For: 1.2.3.4`, `5.6.7.8` on every request, resetting their token-bucket rate limiter. In our production Nginx configuration, `proxy_set_header X-Real-IP $remote_addr;` ensures that Nginx injects the real TCP connection IP. In `RateLimitingFilter`, we inspect `X-Real-IP` before `X-Forwarded-For`.
- **Where in AMCS:**
  - Nginx config: `frontend/nginx.conf`
  - Filter: `com.amcs.infrastructure.web.security.ratelimit.RateLimitingFilter`

### Q10: How are passwords hashed and stored?
- **Concise Answer:** Using BCrypt with a configurable work factor (strength 12 in production, 4 in test environments for fast test execution) along with mandatory complexity checks.
- **Deep Dive:** `SecurityCryptoConfig` configures `BCryptPasswordEncoder`. A custom `PasswordValidator` validates that new passwords meet length (min 8, max 128) and complexity criteria (uppercase, lowercase, digits, special characters) before hashing.
- **Where in AMCS:** `com.amcs.infrastructure.web.security.config.SecurityCryptoConfig` and `com.amcs.application.service.PasswordValidator`

---

## 4. Database & Persistence

### Q11: Why PostgreSQL 16 and Flyway instead of Hibernate `ddl-auto: update`?
- **Concise Answer:** Hibernate's `ddl-auto: update` is non-deterministic, does not support safe column renames or complex rollback strategies, and risks accidental data loss in production. Flyway guarantees repeatable, versioned schema migrations executed in sequence.
- **Deep Dive:** In production (`application-prod.yml`), Hibernate is configured with `ddl-auto: validate`. All tables, indexes, constraints, and audit columns are defined in explicit SQL migration files (`src/main/resources/db/migration/V1__initial_schema.sql`, etc.). Flyway tracks applied migrations in the `flyway_schema_history` table, ensuring exact consistency across development, CI, and production.
- **Where in AMCS:**
  - Migrations: `src/main/resources/db/migration/`
  - Prod config: `src/main/resources/application-prod.yml`

### Q12: What is Optimistic Locking, and where is it applied in AMCS?
- **Concise Answer:** Optimistic locking detects concurrent modifications without locking database rows upfront. In AMCS, it is applied to the `SessionEntity` via a `@Version` column to prevent lost updates during simultaneous roll calls.
- **Deep Dive:** If two faculty members open the same laboratory session roster simultaneously, without concurrency control, the last person to click "Save" would overwrite the first person's attendance entries. By annotating the session version with `@Version`, Hibernate includes `WHERE version = ?` during the `UPDATE` query. If the version changed, an `OptimisticLockException` is thrown, returning an HTTP `409 Conflict` to notify the second user to refresh their view.
- **Where in AMCS:** `com.amcs.infrastructure.persistence.entity.SessionEntity` (`@Version private Long version;`)

### Q13: What N+1 query problem was discovered during the audit, and how was it solved?
- **Concise Answer:** Iterating over enrollments in memory using `findAll().stream().filter(...)` triggered hundreds of queries. It was replaced with targeted repository queries using `@Query` with specific joins.
- **Deep Dive:** During the production-readiness audit, `EnrollmentPersistenceAdapter.findActiveEnrollment()` loaded all system enrollments into JVM memory. We created `findCurrentByStudentId(studentId, currentDate)` directly in `EnrollmentJpaRepository` with indexed lookup on `(student_id, start_date, end_date)`. This reduced the database round-trips from $O(N)$ to $O(1)$.
- **Where in AMCS:**
  - Repository: `com.amcs.infrastructure.persistence.repository.EnrollmentJpaRepository`
  - Adapter: `com.amcs.infrastructure.persistence.adapter.EnrollmentPersistenceAdapter`

---

## 5. Attendance Calculation & Domain Rules

### Q14: How is attendance mathematically calculated when a subject has both Theory and Lab sessions?
- **Concise Answer:** Each session has a `unitCount` (e.g. 1.0 for Theory, 2.0 for Lab). The attendance percentage is the ratio of total attended units to total conducted units, not a simple average of class counts.
- **Deep Dive:** If a student attends 10 of 10 Theory classes (1 unit each = 10 units) and misses 2 of 2 Labs (2 units each = 4 units), a simple count ratio gives $\frac{10}{12} = 83.3\%$. The unit-weighted calculation gives:
  $$\text{Percentage} = \frac{10 \times 1.0 + 0 \times 2.0}{10 \times 1.0 + 2 \times 2.0} \times 100 = \frac{10}{14} \times 100 = 71.4\%$$
  This difference can mean passing versus exam debarment. AMCS uses `BigDecimal` arithmetic with `RoundingMode.HALF_UP` to guarantee legal and mathematical accuracy.
- **Where in AMCS:** `com.amcs.domain.calculation.IntegratedSubjectCalculator`

### Q15: How does AMCS handle student section transfers mid-semester?
- **Concise Answer:** Through a temporal enrollment model where enrollments have `startDate` and `endDate`. Attendance is computed only against sessions falling within the student's active enrollment window for that section.
- **Deep Dive:** When a student transfers from Section A to Section B on October 1st:
  1. The Section A enrollment is closed with `endDate = 2026-09-30` and status `TRANSFERRED`.
  2. A new Section B enrollment is opened with `startDate = 2026-10-01` and status `ACTIVE`.
  3. When calculating Section B attendance, sessions conducted before October 1st are excluded from the student's conducted units count, ensuring the student is never penalized for sessions held before they joined the section.
- **Where in AMCS:**
  - Domain model: `com.amcs.domain.model.SectionEnrollment`
  - Application service: `com.amcs.application.service.EnrollmentApplicationService`

### Q16: How are attendance corrections audited?
- **Concise Answer:** Conducted session records cannot be silently overwritten. Any adjustment requires submitting an audited correction with a mandatory reason, which is permanently logged in `attendance_corrections`.
- **Deep Dive:** `AttendanceRecordingApplicationService.submitCorrection()` validates that the session is in `CONDUCTED` status and not closed. It updates the live `attendance_records` row and creates an immutable `AttendanceCorrectionEntity` recording `recordId`, `originalStatus`, `newStatus`, `reason`, `approverId`, and `correctedAt`.
- **Where in AMCS:**
  - Entity: `com.amcs.infrastructure.persistence.entity.AttendanceCorrectionEntity`
  - Service: `com.amcs.application.service.AttendanceRecordingApplicationService`

---

## 6. Excel Processing & Security

### Q17: Why did you use Apache POI `SXSSFWorkbook` instead of standard `XSSFWorkbook` for reports?
- **Concise Answer:** Standard `XSSFWorkbook` builds the entire XML document in JVM heap memory, causing `OutOfMemoryError` on large rosters. `SXSSFWorkbook` streams rows to disk using a sliding window, keeping memory footprint constant ($O(1)$).
- **Deep Dive:** Generating university-wide attendance sheets for thousands of students can create thousands of rows with tens of thousands of cells. `SXSSFWorkbook` keeps only a fixed window (e.g., 100 rows) in memory, flushing older rows to temporary files on disk. The resulting binary XLSX is streamed directly to the HTTP output stream.
- **Where in AMCS:** `com.amcs.infrastructure.excel.exporter.Rpt001ExcelExporter` and `Rpt004ExcelExporter`

### Q18: What security protections are applied to spreadsheet imports and exports?
- **Concise Answer:** Three critical defenses: ZIP-bomb expansion ratio checks, disabling XML External Entities (XXE) in StAX readers, and sanitizing Formula Injection (CWE-1236) characters.
- **Deep Dive:** 
  - **ZIP-Bomb:** `.xlsx` files are ZIP archives. We verify compression ratios do not exceed $20.0$.
  - **XXE:** In `StaxExcelImporter`, `XMLInputFactory` is explicitly configured with `IS_SUPPORTING_EXTERNAL_ENTITIES = false` to prevent local file inclusion attacks.
  - **Formula Injection:** Exported strings starting with `=`, `+`, `-`, or `@` are escaped with a leading single apostrophe (`'`) so Excel interprets them as literal text instead of executing formulas.
- **Where in AMCS:** `com.amcs.infrastructure.excel.importer.ExcelSecurityValidator`

---

## 7. Performance & Concurrency

### Q19: Why is in-memory rate limiting acceptable for a single instance but insufficient for multiple replicas?
- **Concise Answer:** In-memory rate limiting stores token buckets in JVM `ConcurrentHashMap`. With multiple replicas, each node has its own isolated memory, allowing an attacker to bypass the rate limit by spreading requests across replicas.
- **Deep Dive:** In a single-instance container, in-memory rate limiting with Bucket4j is zero-latency, requires no external infrastructure, and has no network failure modes. However, if horizontally scaled behind a round-robin load balancer across 3 instances, an attacker effectively gets $3 \times$ the configured rate limit. For multi-replica architectures, rate limiting state should be centralized in Redis (`Bucket4j-Redis`) or offloaded to an edge API gateway / Cloudflare WAF.
- **Where in AMCS:** Documented in `README.md` and `docs/ARCHITECTURE.md`.

### Q20: How would you scale this system to 100,000+ active students?
- **Concise Answer:** Read/write database splitting, Redis for caching and distributed rate limiting, and asynchronous background queues for bulk Excel generation.
- **Deep Dive:**
  1. **Database:** Implement PostgreSQL read replicas with Spring's `AbstractRoutingDataSource` to route read-only calculation queries away from the primary writer node.
  2. **Caching:** Cache active attendance policies and academic metadata in Redis, as they are read frequently but rarely updated.
  3. **Async Reports:** Move large campus-wide report exports from synchronous HTTP requests to asynchronous jobs using RabbitMQ/Kafka workers, uploading the generated spreadsheet to S3 and notifying the user.
- **Where in AMCS:** Covered in `docs/ARCHITECTURE.md` Section 12.

---

## 8. Deployment & Containers

### Q21: Why use multi-stage Docker builds?
- **Concise Answer:** Multi-stage builds separate the build environment (Maven, JDK, source code) from the runtime environment, resulting in a minimal, lightweight image containing only the JRE and compiled JAR, reducing attack surface and image size.
- **Deep Dive:** The builder stage uses `maven:3.9.9-eclipse-temurin-21-alpine` to compile code and resolve dependencies. The runtime stage starts fresh from `eclipse-temurin:21-jre-alpine` (under 100MB) and copies only the compiled JAR. Build tools, Git history, and Maven cache are excluded from the final image.
- **Where in AMCS:** `Dockerfile` (root) and `frontend/Dockerfile`

### Q22: Why run containers as a non-root user?
- **Concise Answer:** Running as root inside a container creates severe security risks; if an attacker escapes the container via a Linux kernel vulnerability, they immediately obtain root access on the host system.
- **Deep Dive:** In our `Dockerfile`, we create a dedicated system user and group:
  ```dockerfile
  RUN addgroup -g 10001 -S amcs && adduser -u 10001 -S amcs -G amcs
  USER amcs:amcs
  ```
  Even if an application vulnerability (such as RCE) occurs, the attacker cannot modify system binaries, install packages, or escalate privileges on the host.
- **Where in AMCS:** `Dockerfile` lines 20–25.

### Q23: Why do we use Nginx in front of Spring Boot and React?
- **Concise Answer:** Nginx acts as an efficient static file server for the React bundle with gzip compression and caching, handles SPA HTML5 history routing fallback (`try_files`), and acts as a reverse proxy for `/api/*` and `/actuator/health`.
- **Deep Dive:** Nginx is significantly more efficient than Spring Boot or Node.js at serving static files (`/assets/*`). By serving both the frontend and proxying `/api/` under the same hostname and port, we completely eliminate CORS overhead in production and provide a single ingress point for SSL termination.
- **Where in AMCS:** `frontend/nginx.conf`

---

## 9. Testing Strategy

### Q24: What do the 567 backend tests cover?
- **Concise Answer:** The suite covers pure domain mathematical calculations, invariants, entity boundary rules, application services, mock repository persistence, Spring Security filters, and full mock MVC REST controller contracts.
- **Deep Dive:**
  - **Domain Tests (~250 tests):** Test pure calculation engines with edge cases (zero conducted classes, missing attendance, decimal rounding, lab multipliers).
  - **Application Service Tests (~180 tests):** Test orchestration, transaction semantics, and authorization rules using Mockito.
  - **Infrastructure/Security Tests (~137 tests):** Test Spring Security filter chains, JWT generation/revocation, rate-limiting headers, and REST controller endpoints with `MockMvc`.
- **Where in AMCS:** `src/test/java/com/amcs/**`

### Q25: What is the difference between unit, integration, and end-to-end tests in this codebase?
- **Concise Answer:** 
  - Unit tests verify pure functions and single classes in isolation with zero Spring context (e.g. `AttendanceCalculationEngineTest`).
  - Integration tests verify interaction between multiple components or Spring context (e.g. `ComprehensiveSecurityAndRbacTest`).
  - End-to-end tests verify the full live container stack including PostgreSQL, Nginx, and real HTTP requests (e.g. `verify_production_stack.py`).
- **Where in AMCS:**
  - Unit: `src/test/java/com/amcs/domain/calculation/*`
  - Integration: `src/test/java/com/amcs/infrastructure/web/controller/*`
  - End-to-End: `verify_production_stack.py`
