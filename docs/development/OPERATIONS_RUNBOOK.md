# AMCS Production Operations Runbook

**Attendance Management and Calculation System (AMCS)**  
Target Environment: Docker / Container Orchestration, PostgreSQL 16, Java 21 LTS

---

## 1. System Overview & Architecture Topology

AMCS is a mission-critical university attendance tracking and regulatory calculation engine built with a Hexagonal Architecture (Ports and Adapters).

```
                             [ University Clients / Frontend SPA ]
                                             │
                                     (HTTPS / TLS 1.3)
                                             ▼
                             ┌───────────────────────────────┐
                             │    Reverse Proxy / Ingress    │
                             │ (Nginx / Cloudflare / AWS ALB)│
                             └───────────────┬───────────────┘
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
         ┌───────────────────────────┐               ┌───────────────────────────┐
         │ AMCS Backend Instance 1   │               │ AMCS Backend Instance 2   │
         │ (Java 21 JRE / Non-Root)  │               │ (Java 21 JRE / Non-Root)  │
         │ Port 8080                 │               │ Port 8080                 │
         └─────────────┬─────────────┘               └─────────────┬─────────────┘
                       │                                           │
                       │   HikariCP Pool (Max: 20 per instance)    │
                       └─────────────────────┬─────────────────────┘
                                             ▼
                             ┌───────────────────────────────┐
                             │      PostgreSQL 16 Engine     │
                             │  (Persistent NVMe Storage)    │
                             │ Port 5432                     │
                             └───────────────────────────────┘
```

---

## 2. Environment Variables & Secrets Checklist

| Variable Name | Required? | Default / Example | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` | Activates production configuration profile |
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://amcs-postgres:5432/amcs_db` | PostgreSQL JDBC connection URL |
| `POSTGRES_USER` | Yes | `amcs_user` | Database user account |
| `POSTGRES_PASSWORD` | **Yes (Secret)** | *No default* (Fail-fast if missing) | Database authentication secret |
| `AMCS_JWT_SECRET` | **Yes (Secret)** | *No default* (Fail-fast if missing, min 256 bits / 32 bytes) | HMAC-SHA256 signing secret for JWT tokens |
| `AMCS_JWT_ISSUER` | No | `amcs-auth-service` | Token issuer identifier |
| `AMCS_JWT_EXPIRATION_SECONDS` | No | `3600` (1 hour) | Access token lifespan |
| `AMCS_CORS_ALLOWED_ORIGINS` | Yes | `https://amcs.university.edu` | Comma-separated list of allowed frontend origins |
| `SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE` | No | `20` | Maximum database connections in HikariCP pool |
| `SPRING_DATASOURCE_HIKARI_MIN_IDLE` | No | `5` | Minimum idle connections in HikariCP pool |
| `AMCS_RATE_LIMIT_AUTH` | No | `10` | Max auth attempts per minute per IP |
| `AMCS_RATE_LIMIT_HEAVY` | No | `25` | Max bulk imports/reports per minute per client |

---

## 3. Production Deployment Procedures

### 3.1 Initial Deployment with Docker Compose
```bash
# 1. Create directory and copy configuration files
mkdir -p /opt/amcs && cd /opt/amcs

# 2. Copy docker-compose.prod.yml, Dockerfile, and production environment file (.env.prod)
# 3. Secure environment file permissions
chmod 600 .env.prod

# 4. Pull/Build and start containers in background
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# 5. Verify healthy status
docker compose -f docker-compose.prod.yml ps
```

### 3.2 Verifying Liveness and Readiness Probes
```bash
# Verify overall health (must return HTTP 200 and {"status":"UP"})
curl -s -i http://localhost:8080/actuator/health

# Verify liveness probe
curl -s -i http://localhost:8080/actuator/health/liveness

# Verify readiness probe
curl -s -i http://localhost:8080/actuator/health/readiness
```

---

## 4. Backup and Restore Procedures

### 4.1 Automated Daily Database Backup (`pg_dump`)
Run this backup command via cron or systemd timer every night:
```bash
#!/bin/bash
set -euo pipefail

BACKUP_DIR="/var/backups/amcs"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/amcs_backup_${TIMESTAMP}.dump"

mkdir -p "${BACKUP_DIR}"

# Execute custom-format binary dump with gzip compression (-Fc)
docker exec amcs-postgres-prod pg_dump -U amcs_user -d amcs_db -Fc -f "/tmp/backup.dump"
docker cp amcs-postgres-prod:/tmp/backup.dump "${BACKUP_FILE}"
docker exec amcs-postgres-prod rm -f "/tmp/backup.dump"

# Restrict permissions
chmod 600 "${BACKUP_FILE}"

# Retain backups for 30 days
find "${BACKUP_DIR}" -type f -name "amcs_backup_*.dump" -mtime +30 -delete

echo "AMCS backup successfully completed: ${BACKUP_FILE}"
```

### 4.2 Step-by-Step Restoration Procedure
```bash
# 1. Stop backend container to prevent concurrent database writes
docker compose -f docker-compose.prod.yml stop amcs-backend

# 2. Copy target dump file into postgres container
docker cp /var/backups/amcs/amcs_backup_20260830_120000.dump amcs-postgres-prod:/tmp/restore.dump

# 3. Drop existing connections and restore database
docker exec -i amcs-postgres-prod psql -U amcs_user -d postgres << 'EOF'
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'amcs_db' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS amcs_db;
CREATE DATABASE amcs_db OWNER amcs_user;
EOF

# 4. Restore data from binary dump
docker exec amcs-postgres-prod pg_restore -U amcs_user -d amcs_db --clean --if-exists /tmp/restore.dump

# 5. Clean up temporary dump in container
docker exec amcs-postgres-prod rm -f /tmp/restore.dump

# 6. Restart backend and verify health
docker compose -f docker-compose.prod.yml start amcs-backend
curl -s http://localhost:8080/actuator/health
```

---

## 5. Performance Tuning Guidelines

### 5.1 HikariCP Connection Pool Sizing
- Rule of Thumb: `connections = ((core_count * 2) + effective_spindle_count)`
- For 4 vCPU PostgreSQL node: default `maximum-pool-size: 20` is optimal.
- `connection-timeout: 30000ms`: Prevents threads from hanging indefinitely when pool is saturated.
- `leak-detection-threshold: 20000ms`: Logs stack traces for unclosed connections.

### 5.2 JVM Memory Flags
Configured in `Dockerfile`:
```
-XX:+UseG1GC
-XX:MaxRAMPercentage=75.0
-Djava.security.egd=file:/dev/./urandom
```
- Container memory limit: Allocate at least 1.5 GB RAM to the backend container (heap will dynamically size to ~1.1 GB).

---

## 6. Incident Triage Runbooks

### Incident 1: HikariCP Connection Pool Exhaustion
- **Symptoms**: High latency, logs contain `Connection is not available, request timed out after 30000ms`.
- **Diagnosis**:
  ```bash
  # Check active postgres connections
  docker exec -it amcs-postgres-prod psql -U amcs_user -d amcs_db -c "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"
  # Check for long-running transactions
  docker exec -it amcs-postgres-prod psql -U amcs_user -d amcs_db -c "SELECT pid, now() - query_start AS duration, query FROM pg_stat_activity WHERE state != 'idle' ORDER BY duration DESC;"
  ```
- **Remediation**:
  1. Terminate long-running locking query: `SELECT pg_cancel_backend(<pid>);`
  2. Increase pool size if database server has headroom: `SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE=30`.

### Incident 2: Out of Memory (OOM) or Temp File Saturation
- **Symptoms**: Container restarts with exit code 137 (OOMKilled) or disk space alarms.
- **Diagnosis**:
  1. Inspect POI temp disk directory: All streaming components (`StreamingExcelGenerator`, `StreamingReportExcelGenerator`) call `.dispose()` inside `try-finally` blocks.
  2. Inspect heap dump or metrics: `curl -H "Authorization: Bearer <ADMIN_TOKEN>" http://localhost:8080/actuator/metrics/jvm.memory.used`.

### Incident 3: Rate Limiting Surges (HTTP 429 Alerts)
- **Symptoms**: Legitimate users receive HTTP 429 `"RATE_LIMIT_EXCEEDED"`.
- **Diagnosis**:
  1. Verify client IP forwarding in reverse proxy (`X-Forwarded-For`). If reverse proxy does not forward original IP, all requests appear from proxy IP!
  2. Ensure `X-Forwarded-For` header is set in Nginx/ALB:
     ```nginx
     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     ```
  3. Temporarily raise limits if required: `AMCS_RATE_LIMIT_AUTH=30` and `AMCS_RATE_LIMIT_HEAVY=60`.
