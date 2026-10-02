# AMCS Production Deployment & Release Engineering Runbook

**Release Version:** v8.0.0  
**Target Platform:** Linux (Ubuntu 22.04+ / RHEL 9+ / Alpine), Docker Engine 24+, Docker Compose v2+  
**Architecture:** Multi-container (PostgreSQL 16, Spring Boot 3.3.3 / Java 21, Nginx 1.27 + React 19)

---

## 1. Pre-Deployment Checklist

Complete every item in this checklist prior to initiating deployment to staging or production.

### 1.1 Secrets & Cryptographic Keys
- [ ] **Generate Production JWT Secret:**
  ```bash
  # Must be at least 256 bits (32 bytes). Generates 48 bytes base64:
  openssl rand -base64 48
  ```
  *Ensure this is set to `AMCS_JWT_SECRET` in your deployment secrets store.*
- [ ] **Verify Fallback Secret is Rejected:** Confirm the application fails fast at startup if the development fallback secret is used with `SPRING_PROFILES_ACTIVE=prod`.
- [ ] **Generate Strong Database Credentials:**
  ```bash
  openssl rand -hex 24
  ```
- [ ] **Store Secrets Securely:** Ensure secrets are supplied via Docker secrets, Kubernetes secrets, or environment variables in a protected `.env` file (permissions `chmod 600 .env`). **Never commit secrets to Git.**

### 1.2 Database & Migrations
- [ ] **PostgreSQL 16 Provisioning:** Verify persistent volume storage is available (`amcs_prod_postgres_data`).
- [ ] **Flyway Ownership:** Verify Flyway migrations (`V1` through `V5`) are in `src/main/resources/db/migration`. Flyway owns all schema creation and evolutions.
- [ ] **Hibernate Validation Only:** Verify `spring.jpa.hibernate.ddl-auto` is set to `validate`. Hibernate must NEVER auto-generate or alter tables in production.
- [ ] **Backup Plan Initialized:** Set up automated periodic snapshots or `pg_dump` cron:
  ```bash
  docker exec amcs-postgres-prod pg_dump -U $POSTGRES_USER -d $POSTGRES_DB -F c -b -v -f /backups/amcs_$(date +%Y%m%d_%H%M%S).dump
  ```

### 1.3 Networking, Reverse Proxy & CORS
- [ ] **Bootstrap Disabled:** Verify `AMCS_BOOTSTRAP_ENABLED=false` so test accounts and demo departments are never seeded in production.
- [ ] **CORS Configuration:** Configure `AMCS_CORS_ALLOWED_ORIGINS` with the exact HTTPS origin(s) of the application (e.g. `https://amcs.university.edu`).
- [ ] **Domain & DNS:** Point your institutional domain / subdomain DNS `A`/`CNAME` records to the host public IP.
- [ ] **SSL / TLS Certificate:** Prepare valid SSL certificates (e.g. Let's Encrypt / Certbot or institutional PKI) at `/etc/nginx/ssl/cert.pem` and `/etc/nginx/ssl/key.pem`.
- [ ] **Reverse Proxy IP Trust:** The bundled Nginx gateway unconditionally overwrites `X-Real-IP` and `X-Forwarded-For` with `$remote_addr` to protect the backend rate limiter against header spoofing.

---

## 2. Deployment Execution Procedure

Follow these sequential steps to perform a zero-downtime or fresh deployment:

### Step 2.1: Prepare Environment Configuration
```bash
# Clone or pull target release tag
git checkout v8.0.0

# Copy production environment template
cp .env.example .env
chmod 600 .env

# Edit .env with your generated secrets and institutional values
nano .env
```

Ensure the following variables are set in `.env`:
```env
POSTGRES_DB=amcs_db
POSTGRES_USER=amcs_db_admin
POSTGRES_PASSWORD=<generated-secure-password>
AMCS_JWT_SECRET=<generated-48-bytes-secret>
AMCS_JWT_ISSUER=amcs-auth-service
AMCS_JWT_EXPIRATION_SECONDS=3600
AMCS_CORS_ALLOWED_ORIGINS=https://amcs.university.edu
AMCS_BOOTSTRAP_ENABLED=false
HTTP_PORT=80
HTTPS_PORT=443
```

### Step 2.2: Build and Launch Services
```bash
# Build production images and start the stack in detached mode
docker compose -f docker-compose.prod.yml up -d --build
```

### Step 2.3: Monitor Service Boot Sequence
```bash
# Watch container startup and health checks
docker compose -f docker-compose.prod.yml ps

# View backend startup logs to verify Flyway migrations executed
docker compose -f docker-compose.prod.yml logs -f amcs-backend
```

Expected startup indicators in backend log:
```
Flyway Community Edition 10.x.x by Redgate
Database: jdbc:postgresql://amcs-postgres:5432/amcs_db (PostgreSQL 16.x)
Successfully validated 5 migrations
Current version of schema "public": 5
Schema "public" is up to date. No migration necessary.
Started AmcsApplication in X.XXX seconds
```

### Step 2.4: Verify Container Health Checks
```bash
# Check Docker healthcheck status (all should report 'healthy')
docker inspect --format='{{.Name}}: {{.State.Health.Status}}' $(docker compose -f docker-compose.prod.yml ps -q)

# Direct HTTP health probe
curl -I http://localhost/actuator/health
# Expected: HTTP/1.1 200 OK
# Body: {"status":"UP"}

curl -I http://localhost/healthz
# Expected: HTTP/1.1 200 OK (OK)
```

---

## 3. Post-Deployment Verification & Smoke Testing

Execute this smoke test suite immediately after deployment:

### 3.1 Health & Connectivity
- [ ] `GET /actuator/health` returns `{"status":"UP"}` with database connection verified.
- [ ] `GET /healthz` on port 80/443 returns 200 OK from Nginx.

### 3.2 Authentication & Security
- [ ] Access the application in browser: `https://<domain>/`.
- [ ] Verify HTTP automatically redirects to HTTPS (when TLS enabled).
- [ ] Verify `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, and `Strict-Transport-Security` headers are present in response.
- [ ] Authenticate with administrative credentials. Verify JWT token is received and user profile is returned.
- [ ] Attempt login with invalid credentials 5 times; verify user lockout or rate limiting activates as configured.

### 3.3 Administrative Flow (HOD_ADMIN)
- [ ] Create an Academic Period (e.g. `2026-2027 Odd Semester`).
- [ ] Create a Department (e.g. `Computer Science & Engineering`).
- [ ] Create Sections and Curriculum Subjects.
- [ ] Create Faculty accounts and assign teaching sections (`faculty_assignments`).
- [ ] Enroll students into sections.

### 3.4 Faculty Flow (FACULTY)
- [ ] Log in as a faculty member.
- [ ] View assigned timetable/sections; verify teaching scope boundaries enforce that unassigned subjects/sections are inaccessible.
- [ ] Conduct roll-call for a scheduled session; submit batch attendance.
- [ ] Verify attendance records are persisted in database (`SELECT COUNT(*) FROM attendance_records`).
- [ ] Submit an audited correction with justification reason.

### 3.5 Student Self-Service Flow (STUDENT)
- [ ] Log in as an enrolled student.
- [ ] View overall and subject-level attendance percentages on Dashboard.
- [ ] Open Shortage Projection modal and calculate future sessions required to attain 75% or 85% threshold.
- [ ] Verify IDOR protection: Attempt to access another student's ID via calculation API; verify HTTP 403 Forbidden is returned.

### 3.6 Reports & Ingestion Engine
- [ ] Download Student Attendance Summary (RPT-001) as `.xlsx`.
- [ ] Download Subject Attendance Register (RPT-004) as `.xlsx`.
- [ ] Verify spreadsheet opens cleanly with proper number formats, formulas, and headers.
- [ ] Test bulk import template download (`GET /api/v1/imports/templates/STUDENTS`).

---

## 4. Disaster Recovery & Rollback Procedure

### 4.1 Rolling Back to Previous Release
If an unexpected critical issue occurs after deployment:
```bash
# 1. Stop the current production stack
docker compose -f docker-compose.prod.yml down

# 2. Checkout previous known-good tag
git checkout v7.0.0

# 3. Restore database snapshot if schema migration failed
docker compose -f docker-compose.prod.yml up -d amcs-postgres
cat /backups/amcs_pre_deploy.sql | docker exec -i amcs-postgres-prod psql -U $POSTGRES_USER -d $POSTGRES_DB

# 4. Relaunch previous release
docker compose -f docker-compose.prod.yml up -d --build
```

### 4.2 Database Backup & Restore Commands
```bash
# Full compressed backup
docker exec -t amcs-postgres-prod pg_dump -U $POSTGRES_USER -d $POSTGRES_DB -c -F c -f /tmp/backup.dump
docker cp amcs-postgres-prod:/tmp/backup.dump ./amcs_backup_$(date +%Y%m%d).dump

# Full restore from dump
docker cp ./amcs_backup.dump amcs-postgres-prod:/tmp/backup.dump
docker exec -i amcs-postgres-prod pg_restore -U $POSTGRES_USER -d $POSTGRES_DB --clean --if-exists /tmp/backup.dump
```
