# AMCS v2 — Attendance Management & Calculation System

AMCS v2 is a fast, robust attendance management system built with Spring Boot, PostgreSQL, Flyway, and React/TypeScript. It digitalizes institutional roll-call attendance, supporting academic calendars, theoretical lectures, lab batch allocations, timetable enforcement, and real-time attendance calculations.

---

## 1. Prerequisites

- **Java**: 21+ (OpenJDK 21 or newer)
- **Maven**: 3.9+
- **Node.js**: 20+ and `npm`
- **PostgreSQL**: 16+ (Local or Docker)

---

## 2. PostgreSQL Setup & Database Creation

You can run PostgreSQL natively or using Docker.

### Option A: Using Docker (Recommended)

```bash
docker run -d \
  --name amcs-postgres \
  -e POSTGRES_DB=amcs_db \
  -e POSTGRES_USER=amcs_user \
  -e POSTGRES_PASSWORD=amcs_secret_password \
  -p 5432:5432 \
  postgres:16-alpine
```

### Option B: Local PostgreSQL (psql)

Connect to your local PostgreSQL instance as superuser:

```sql
CREATE USER amcs_user WITH PASSWORD 'amcs_secret_password';
CREATE DATABASE amcs_db OWNER amcs_user;
GRANT ALL PRIVILEGES ON DATABASE amcs_db TO amcs_user;
```

---

## 3. Backend Configuration & Migrations

Configuration is managed via `src/main/resources/application.yml` and overridable via standard environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/amcs_db` | PostgreSQL JDBC URL |
| `POSTGRES_USER` | `amcs_user` | Database user |
| `POSTGRES_PASSWORD` | `amcs_secret_password` | Database password |
| `JWT_SECRET` | `amcs-v2-dev-secret-key-must-be-at-least-32-characters-long!` | Secret key for JWT signing |
| `CORS_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173,http://[::1]:5173` | Allowed CORS origins |

### Flyway Database Migrations

Flyway automatically applies all versioned migrations on application startup:
- `V1`: Baseline schema
- `V2`: AMCS core relational tables (terms, departments, sections, subjects, faculty, students, lab batches, timetable entries, sessions, attendance records)
- `V3`: Seed administrative data, initial faculty, and sample timetables
- `V4`: Seed 124-student scalable dataset (`325106410001`–`325106410124`) for Section `CSE-A1`
- `V5`: Standardized user password BCrypt hashes (`password123`)
- `V6`: Academic calendar holidays table (including national declared holidays like Gandhi Jayanti)

To run automated backend tests and verify migrations against a clean database:
```bash
mvn clean test
```

---

## 4. Starting the Application

### 1. Start the Backend

```bash
mvn spring-boot:run
```

The Spring Boot backend will start on **`http://localhost:8080`**.

### 2. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The React + Vite frontend will start on **`http://localhost:5173`**.

---

## 5. Default Development Credentials

The database is seeded with ready-to-use development accounts:

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `password123` | Full access to curriculum, departments, sections, faculty, students, roster enrollment, timetable, holidays, and schedule JSON import |
| **Faculty 1** | `faculty1` | `password123` | Dr. Ravi Kumar — Assigned to `OS` (Theory), `OOPJ-LAB` (Batch 2 & 3), `OS-LAB` (Batch 1), `COA` (Theory) |
| **Faculty 2** | `faculty2` | `password123` | Prof. Anitha Reddy — Assigned to `DMS` (Theory), `PSQT` (Theory), `OS-LAB` (Batch 2), `OOPJ-LAB` (Batch 1) |

*Quick Demo Access buttons on the Login page allow instant sign-in.*

---

## 6. How to Import a Schedule (Admin Workflow)

1. Sign in as **`admin`**.
2. Navigate to the **Academic Setup** screen.
3. Click the **Import Schedule** button in the top action bar.
4. **Copy LLM Prompt**: Click the "Copy LLM Prompt" tab if you want to extract structured schedule JSON from an existing syllabus or routine document.
5. **Paste Schedule JSON**: Paste the JSON schema containing academic terms, departments, sections, faculty profiles, subjects (theory/lab), lab batches, and timetable entries into the JSON editor.
6. **Preview**: Click **Preview Schedule Import**. The system executes strict validation:
   - Validates entity hierarchy (terms, departments, sections, subjects, faculty, batches).
   - Validates class timings (`endTime > startTime`, no Sunday allocations).
   - Flags existing entities that will be safely reused.
   - Flags new faculty that will be auto-created with locked accounts pending credential setup.
   - Previews are strictly read-only and will not mutate database state.
7. **Commit**: Click **Apply & Commit Schedule**. The commit is executed within a single transactional boundary (`@Transactional`). If any conflict occurs, the entire transaction is rolled back cleanly.

---

## 7. How the Class Representative (CR) / Faculty Uses the System

1. **Sign In**: Log in using your faculty credentials (or assigned account).
2. **Dashboard Overview**:
   - Check the **Institutional Calendar Banner** (displays working day status, or indicates non-working days such as Sundays, Second Saturdays, or declared official holidays).
   - View **Today's Classes** allocated specifically to your account.
3. **Open an Attendance Session**:
   - Click **Take Attendance** on today's class card.
   - The system retrieves the section roster (or specific lab batch roster for lab classes).
4. **Marking Attendance**:
   - By default, all students are marked **Present**.
   - Tap or click individual students (or use quick toggles) to mark **Absent**.
   - Review total present and absent counts displayed at the bottom bar.
5. **Submit & Lock**:
   - Click **Submit Attendance**.
   - Once submitted, the session is atomically recorded and becomes **immutable** to prevent tampering.
6. **Review Reports**:
   - **Register (Detailed Grid)**: View date-by-date student attendance columns.
   - **Monthly Summary**: View month-wise totals and percentages.
   - **Subject-wise Summary**: View combined theory + lab percentages.
   - **Overall Summary**: View overall attendance standing, warning badges, and eligibility status.

---

## 8. Verification & Test Suite

- Backend Tests:
  ```bash
  mvn test
  ```
- Frontend Tests:
  ```bash
  cd frontend && npm test -- --run
  ```
- Frontend Production Build:
  ```bash
  cd frontend && npm run build
  ```
