# AMCS Professional Resume & Portfolio Bullets

Use these verified, high-impact bullet points tailored to your target engineering roles. All metrics and architectural claims reflect actual implementations in the repository.

---

## Version A: Java Backend Focused

**Attendance Management & Calculation System (AMCS) | Java 21, Spring Boot 3, PostgreSQL, Docker**
* *System Architecture:* Architected a production-ready university attendance and compliance platform using **Hexagonal Architecture (Ports and Adapters)**, completely decoupling core mathematical calculations from Spring Boot and persistence frameworks.
* *Calculation & Data Modeling:* Engineered a deterministic calculation engine in pure Java 21 handling multi-component credit weighting (Theory/Lab), temporal mid-term section transfers, and predictive shortage simulations using precise `BigDecimal` arithmetic.
* *Security & Revocation:* Implemented stateless JWT authentication with instant token revocation via database-backed `tokenVersion` checks, augmented with service-layer IDOR protection and Token-Bucket rate limiting against brute-force attacks.
* *Concurrency & Persistence:* Designed schema migrations across 15+ relational tables using **Flyway**; eliminated lost updates during concurrent roll-call sessions by implementing JPA `@Version` optimistic concurrency control.
* *High-Performance Reporting:* Built streaming Excel report exporters using **Apache POI SXSSF** and StAX XML parsers, maintaining constant $O(1)$ memory consumption and guarding against ZIP-bomb and XXE attacks.
* *Quality & Testing:* Developed an automated test suite of **567 unit and integration tests** (JUnit 5, Mockito, AssertJ, MockMvc) achieving 100% pass rate across domain invariants, security filters, and REST API contracts.

---

## Version B: Full-Stack Engineering Focused

**Full-Stack Attendance & Compliance Platform (AMCS) | React 19, TypeScript, Spring Boot 3, PostgreSQL**
* *Full-Stack Development:* Engineered an end-to-end institutional web application featuring a **React 19 + TypeScript** SPA frontend and a **Spring Boot 3.3.3 / Java 21** REST API backend.
* *Role-Based Portals:* Designed distinct responsive user portals for Students (real-time % gauges, shortage forecaster), Faculty (interactive roster roll call, audited corrections), and HOD Admins (curriculum setup, policy versioning, bulk imports).
* *API Integration & Type Safety:* Implemented strict client-server contracts with automated error boundaries, Axios interceptors for JWT authorization headers, and unified HTTP RFC 7807 problem details.
* *Containerized Deployment:* Packaged full-stack application into a 3-tier Docker Compose production stack utilizing a multi-stage Alpine build for Spring Boot and an Nginx 1.27 reverse proxy serving the cached React bundle and routing `/api/` traffic.
* *Automated CI/CD:* Established a GitHub Actions pipeline verifying TypeScript compilation, ESLint checks, 8 Vitest frontend tests, 567 Maven backend tests, and Docker container builds on every pull request.

---

## Version C: Software Engineering & System Design Focused

**Attendance Management and Calculation System (AMCS) | Distributed Systems & Architecture**
* *Domain-Driven Design:* Modeled institutional academic governance using pure Java domain aggregates, immutable records, and versioned compliance policies, ensuring zero framework lock-in.
* *Temporal Windowing:* Solved historical attendance attribution in section transfers by designing a temporal enrollment model tracking session dates against student enrollment time windows.
* *Perimeter Security & Anti-Spoofing:* Configured Nginx reverse proxy to overwrite `X-Real-IP` and hardened backend filters against client IP spoofing via manipulated `X-Forwarded-For` headers.
* *Defensive Excel Ingestion:* Architected a two-phase staging and commit spreadsheet import pipeline with automated validation, preventing CSV Formula Injection (CWE-1236) and XML External Entity (XXE) vulnerabilities.
* *Production Reliability:* Verified zero-downtime containerized deployment with healthcheck dependency chaining, non-root container users (UID 10001), graceful SIGTERM shutdown handling, and automated end-to-end smoke testing.
