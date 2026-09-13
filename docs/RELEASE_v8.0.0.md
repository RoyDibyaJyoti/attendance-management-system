# AMCS Release Notes — v8.0.0

**Release Tag:** `v8.0.0`  
**Release Name:** Production Deployment & Release Engineering  
**Release Date:** September 13, 2026  
**Status:** Stable / Production-Ready  

---

## 1. Release Purpose

Release **v8.0.0** elevates the Attendance Management and Calculation System (AMCS) from a verified development state into a fully hardened, containerized, and reproducibly deployable production system. This release formalizes the production infrastructure, establishes a multi-tier container topology, hardens perimeter security, resolves IP spoofing vulnerabilities, configures continuous integration, and provides complete deployment runbooks.

---

## 2. Key Changes & Enhancements

### 2.1 Backend Containerization & JVM Tuning
- **Multi-Stage Dockerfile:** Implemented builder (`maven:3.9.9-eclipse-temurin-21-alpine`) and runtime (`eclipse-temurin:21-jre-alpine`) stages, reducing production image size and attack surface.
- **Least-Privilege Execution:** Created unprivileged system user/group `amcs:amcs` (UID `10001`, GID `10001`) with no shell login (`/sbin/nologin`).
- **Signal Forwarding & Graceful Shutdown:** Executed Java directly as PID 1 (`exec java ...`) with `-XX:MaxRAMPercentage=75.0` and `-XX:+UseG1GC`, paired with Spring Boot `server.shutdown=graceful` for zero dropped in-flight requests.
- **Actuator Health Probe:** Native container health check using `wget --spider http://localhost:8080/actuator/health`.

### 2.2 Frontend Containerization & Reverse Proxy
- **Multi-Stage Frontend Build:** Compiles TypeScript and builds Vite bundle using `node:20-alpine`, serving production static assets via `nginx:1.27-alpine`.
- **Nginx Ingress Architecture:** Single ingress point terminating traffic on port 80 (or port 443 with TLS):
  - Serves cached React SPA with `try_files $uri $uri/ /index.html` fallback.
  - Aggressive static asset caching (`Cache-Control "public, max-age=31536000, immutable"` for `/assets/`).
  - Gzip compression for text, CSS, JS, JSON, and SVG.
  - Proxies `/api/` traffic to `http://amcs-backend:8080`.
  - Proxies `/actuator/health` to `http://amcs-backend:8080/actuator/health` while hiding internal metrics.
  - Independent `/healthz` probe returning `200 OK`.

### 2.3 PostgreSQL 16 & Data Persistence
- Containerized PostgreSQL 16 on Alpine Linux with native `pg_isready` health check.
- Data persistence guaranteed via Docker named volume `amcs_pgdata`.
- Schema ownership managed strictly by Flyway; Hibernate locked to `ddl-auto: validate`.

### 2.4 Orchestration & Health Dependency Chaining
- Implemented `docker-compose.prod.yml` with health check dependencies (`depends_on.condition: service_healthy`):
  1. `amcs-postgres` initializes and achieves healthy state.
  2. `amcs-backend` starts, runs Flyway migrations, and reaches healthy state via Actuator.
  3. `amcs-frontend` starts Nginx and exposes ingress ports.

---

## 3. Security Hardening

1. **Anti-Spoofing Client IP Handling:**
   - Updated Nginx to explicitly overwrite `X-Real-IP $remote_addr;`.
   - Updated `RateLimitingFilter.java` to inspect `X-Real-IP` before `X-Forwarded-For`, preventing attackers from bypassing rate limits by injecting arbitrary IP headers.
2. **Production Secret Injection:**
   - Created sanitized `.env.example` with zero hardcoded credentials.
   - Updated `.gitignore` to prevent any `.env` or `.env.*` file from being committed to Git.
   - Enforced fail-fast validation on startup in `application-prod.yml` if default secrets are used.
3. **HTTP Security Headers:**
   - Injected security headers in Nginx responses: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 1; mode=block`, and `Referrer-Policy: strict-origin-when-cross-origin`.
4. **Actuator Exposure Control:**
   - Restricted public Actuator endpoints to `health` and `info`. Blocked sensitive operational endpoints (`env`, `beans`, `flyway`, `heapdump`) from public gateway routing.

---

## 4. Continuous Integration (CI)

Added automated GitHub Actions workflow (`.github/workflows/ci.yml`) executing on all pull requests and pushes to `main`:
- **Job 1 (backend-test):** Java 21, Maven cache, runs full 567 unit and integration test suite (`mvn clean test -B`).
- **Job 2 (frontend-test):** Node 20, runs `npm ci`, ESLint (`npm run lint`), 8 Vitest tests (`npm test`), and production compilation (`npm run build`).
- **Job 3 (docker-build):** Builds both backend and frontend production images using Docker Buildx and verifies non-root UID 10001.

---

## 5. Verification & Test Metrics

All verification suites executed and verified 100% passing:

| Component | Target / Suite | Verification Result |
|:---|:---|:---:|
| **Backend Unit & Integration** | `mvn test` | **567 / 567 passed** (0 failures, 0 errors) |
| **Frontend Vitest Tests** | `npm test -- --run` | **8 / 8 passed** (0 failures, 0 errors) |
| **Frontend Static Linter** | `npm run lint` | **0 errors** (58 warnings) |
| **Frontend Bundle Compilation** | `npm run build` | **Clean build** (469ms) |
| **Backend Docker Image** | `docker build -t amcs-backend .` | Verified non-root UID 10001 |
| **Frontend Docker Image** | `docker build -t amcs-frontend ./frontend` | Verified Nginx + static assets |
| **Production Stack Smoke Test** | `python3 verify_production_stack.py` | **100% passed** across all 7 verification tiers |

---

## 6. Deployment Requirements & Runbooks

- **Prerequisites:** Docker 24+ and Docker Compose v2+.
- **Configuration:** Copy `.env.example` to `.env` and provide strong passwords, 32+ byte HMAC-SHA256 JWT secret, and CORS allowed domain.
- **Runbook Documentation:** Complete operational checklists, deployment steps, and disaster recovery procedures are available in [PRODUCTION_DEPLOYMENT.md](../PRODUCTION_DEPLOYMENT.md).

---

## 7. Operational Limitations & Roadmap

- **TLS Certificate Termination:** The default reverse proxy runs HTTP on port 80. For internet-facing environments, SSL certificates must be mounted using [`frontend/nginx.ssl.conf.template`](../frontend/nginx.ssl.conf.template) or terminated via a cloud load balancer.
- **Single-Node Rate Limiter:** The in-memory Bucket4j rate limiter is optimized for single-instance deployments. For multi-node horizontal scaling, Redis or API gateway rate limiting should be introduced.
- **Backup Automation:** PostgreSQL database persistence is backed by named volume `amcs_pgdata`. Automated off-site backups via `pg_dump` cron jobs must be managed operationally.
