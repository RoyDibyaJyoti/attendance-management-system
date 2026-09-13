# AMCS 5–7 Minute Live Technical Demonstration Script

This script provides a structured, repeatable live demo walkthrough of the **Attendance Management and Calculation System (AMCS)** for portfolio showcases, technical presentations, and engineering interviews.

---

## Pre-Demo Verification & Setup (1 Minute Before Demo)

Ensure the stack is running in either local development mode or Docker production mode:

```bash
# Production Docker stack option:
docker compose -f docker-compose.prod.yml up -d
curl -I http://localhost/actuator/health   # Should return HTTP 200 UP

# Open browser to:
# http://localhost (Production) or http://localhost:5173 (Dev)
```

**Seeded Demo Credentials:**
- **Student:** `student1` / `StudentPassword123!` (Alice Smith, Reg: `REG2026001`)
- **Faculty:** `faculty1` / `FacultyPassword123!` (Dr. Alan Turing, CSE Department)
- **HOD Admin:** `admin` / `AdminPassword123!` (HOD Administrator)

---

## Minute 0:00 – 0:45 | Introduction & Problem Statement

### Speaker Talking Points:
> *"Today I'm presenting AMCS—the Attendance Management and Calculation System. In higher education, attendance tracking is often managed through error-prone spreadsheets or naive trackers that fail when confronted with institutional complexities like mid-semester section transfers, mixed theory/lab credit weighting, and strict accreditation audit requirements. AMCS is an enterprise platform built with Spring Boot 3, React 19, and PostgreSQL on strict Hexagonal Architecture, ensuring complete mathematical integrity, real-time shortage forecasting, and end-to-end auditability."*

---

## Minute 0:45 – 2:00 | Student Experience: Live Calculation & Forecaster

1. **Action:** Navigate to login page, enter:
   - Username: `student1`
   - Password: `StudentPassword123!`
   - Click **Sign In**.
2. **Action:** View the **Student Attendance Dashboard**.
   - Point out Alice Smith's enrolled section (`CSE-A`) and academic period (`Fall 2026`).
   - Highlight the real-time compliance badge: **100.0% ADEQUATE** against the 75% institutional threshold.
3. **Action:** Inspect the course breakdown:
   - **CS301 Operating Systems:** 2 conducted classes, 2 attended $\rightarrow$ `100.0%`.
   - **CS302L Database Systems Lab:** 0 conducted classes $\rightarrow$ status `UNDEFINED` (cleanly handled zero-denominator edge case).
4. **Action:** Open the **Predictive Shortage Forecaster**:
   - Move the projected remaining classes slider.
   - Explain: *"The forecaster doesn't just show current status; it runs a what-if simulation telling the student exactly how many consecutive future classes they must attend to maintain exam eligibility."*
5. **Action:** Click **Download Attendance Statement (RPT-001)**.
   - Show the generated `.xlsx` file downloaded directly in the browser.
6. **Action:** Click **Logout**.

---

## Minute 2:00 – 3:30 | Faculty Experience: Live Roll Call & Audited Corrections

1. **Action:** Log in as Faculty:
   - Username: `faculty1`
   - Password: `FacultyPassword123!`
2. **Action:** View the **Faculty Dashboard / Timetable**:
   - Point out today's scheduled session for **CS301 Operating Systems** in Section `CSE-A`.
3. **Action:** Click **Conduct Roll Call** on the scheduled session:
   - Show the roster loaded specifically for section `CSE-A` (Alice Smith and Bob Jones).
   - Click **Mark All Present** to demonstrate bulk recording speed.
   - Toggle Bob Jones to **ABSENT** or **DUTY_LEAVE**.
   - Click **Submit Attendance**.
   - Highlight that the session status immediately transitions from `SCHEDULED` to `CONDUCTED`.
4. **Action:** Navigate to **Attendance Corrections**:
   - Explain: *"Once a session is conducted, faculty cannot silently overwrite historical records. If an error occurred, they submit an audited correction with a mandatory reason, ensuring complete legal and compliance traceability."*
5. **Action:** Click **Logout**.

---

## Minute 3:30 – 4:30 | Administrator & HOD: Academic Governance

1. **Action:** Log in as Administrator:
   - Username: `admin`
   - Password: `AdminPassword123!`
2. **Action:** View the **HOD Dashboard**:
   - Show institutional KPIs (total enrolled students, active courses, conducted sessions).
3. **Action:** Navigate to **Academic Structure & Sections**:
   - Show Department `Computer Science & Engineering` and section `CSE-A`.
   - Explain the **Temporal Enrollment Model**: *"When a student transfers between sections mid-term, AMCS preserves their past attendance in Section A while creating a new active enrollment window in Section B. Calculations automatically filter sessions to the exact active window at the time of each class."*
4. **Action:** Navigate to **Reports Center**:
   - Show the suite of 8 official reports (RPT-001 through RPT-008).
   - Generate and download **RPT-004 Attendance Register**.
   - Explain: *"Reports are streamed using Apache POI SXSSF with constant $O(1)$ memory usage, preventing heap exhaustion even with tens of thousands of rows."*

---

## Minute 4:30 – 5:30 | Architecture & Security Walkthrough

1. **Talking Points on Architecture:**
   - *"The backend is structured on strict Hexagonal Architecture. The core calculation engine in `com.amcs.domain.*` has zero framework dependencies—no Spring, no Hibernate, no web libraries. It runs pure, deterministic Java 21 logic."*
   - *"Persistence is decoupled via outbound ports and adapters, and the mutable session entity employs JPA `@Version` optimistic locking to prevent lost updates during concurrent roll calls."*
2. **Talking Points on Security:**
   - *"We use stateless JWT authentication, but solve the classic revocation problem by embedding a database-backed `tokenVersion` check on every request, giving instant multi-device revocation on password change."*
   - *"Service-layer IDOR checks guarantee that students cannot inspect another student's calculations, even if they manipulate URL IDs."*
   - *"Our Nginx reverse proxy overwrites `X-Real-IP`, preventing attackers from spoofing client IPs to bypass rate limiting."*

---

## Minute 5:30 – 6:30 | Production Stack & Verification Evidence

1. **Action:** Open terminal and run:
   ```bash
   docker compose -f docker-compose.prod.yml ps
   ```
2. **Show the 3 running containers:**
   - `amcs-postgres-prod` (healthy, persistent named volume)
   - `amcs-backend-prod` (healthy, non-root user UID 10001, Java 21)
   - `amcs-frontend-prod` (healthy, Nginx 1.27 reverse proxy & cached SPA)
3. **Show test coverage evidence:**
   - *"The system is verified with 567 automated backend tests covering calculation invariants and security, 8 frontend tests, and automated end-to-end smoke verification."*
4. **Conclusion:**
   - *"AMCS v8.0.0 is a fully containerized, reproducible, production-hardened platform ready for deployment."*
   - Invite questions from the interviewer.
