# AMCS Security Threat Model & Risk Analysis

**Document Version:** 1.0.0  
**Phase:** Phase 4 — Security, Authentication & RBAC  
**Methodology:** STRIDE / OWASP Top 10 API Security  

---

## 1. Threat Profile Matrix

| Threat Category | Realistic Academic Threat Vector | Impact | Likelihood | Phase 4 Mitigation Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **Spoofing (Identity)** | Student guessing faculty credentials or brute-forcing weak passwords to mark themselves present. | Critical | Moderate | Adaptive BCrypt (cost=12), rate-limiting login attempts, account lockout after 5 failures, stateless cryptographically signed JWT. |
| **Tampering (Attendance Data)** | Disgruntled student modifying HTTP request to change attendance status on roll call from `ABSENT` to `PRESENT`. | High | High | Strict `ROLE_FACULTY` enforcement, session conductor checks, and database-level `uq_attendance_session_student` uniqueness. |
| **Tampering (Session State)** | Student or unauthorized faculty cancelling or rescheduling classes. | High | Low | State machine transition guards, optimistic locking (`@Version`), and faculty teaching assignment checks. |
| **Repudiation** | Faculty claiming they never marked an absent student, or admin denying changing an attendance record. | High | Moderate | Mandatory `approverId` and reason on all corrections, parent session version increment, and actor logging. |
| **Information Disclosure (IDOR)** | Student A changing UUID in `/api/v1/students/{studentB}/attendance/summary` to spy on peer's attendance shortage or condonation status. | High | Very High | Application-layer ownership check via `CurrentUserPort`: if `role == STUDENT`, `actor.studentId` MUST match URL parameter. |
| **Information Disclosure (Data Leakage)** | Database credentials, SQL errors, or JWT secret leaked in API response. | Critical | Low | Unified `ApiErrorResponse` with sanitized messages; zero stack traces; secrets read from environment variables only. |
| **Elevation of Privilege** | Student forging a JWT claim with `"role": "HOD_ADMIN"` or `"role": "FACULTY"`. | Critical | High | HMAC-SHA256 signature verification with high-entropy secret; tokens signed exclusively by server private key. |
| **Denial of Service** | Flooding calculation endpoint with huge ranges or repeatedly submitting massive attendance batches. | Medium | Moderate | Bounded pagination parameters (`size` clamped between 1 and 100), bounded string fields, and fast stateless token verification. |

---

## 2. In-Depth Attack Vectors & Defense Mechanics

### Attack Vector 1: Insecure Direct Object Reference (IDOR)
- **Attack:** An authenticated student (`Alice`) receives a valid JWT token. Alice notes her own student ID `uuid-alice`. Alice changes the URL to `/api/v1/students/{uuid-bob}/attendance/overall`.
- **Vulnerability if Unchecked:** The API would return Bob's attendance percentage, classification, and shortage risk.
- **AMCS Defense:**
  ```java
  // In AttendanceCalculationApplicationService
  AuthenticatedActor actor = currentUserPort.requireCurrentActor();
  if (actor.isStudent()) {
      UUID selfId = actor.studentId().orElseThrow(() -> new AccessDeniedException("No student ID linked"));
      if (!selfId.equals(studentId)) {
          throw new AccessDeniedException("Access denied: You cannot view attendance records of another student");
      }
  }
  ```
  Returns **HTTP 403 Forbidden** with `ApiErrorResponse` (`code: ACCESS_DENIED`).

### Attack Vector 2: Faculty Impersonation & Unauthorized Roll-Call
- **Attack:** Faculty Member Dr. X teaches CS101. Dr. X discovers session ID `uuid-cs202` taught by Dr. Y. Dr. X submits `POST /api/v1/sessions/{uuid-cs202}/attendance` to mark a student present.
- **AMCS Defense:**
  ```java
  // In AttendanceRecordingApplicationService
  AuthenticatedActor actor = currentUserPort.requireCurrentActor();
  if (actor.isFaculty()) {
      UUID facultyId = actor.facultyId().orElseThrow(() -> new AccessDeniedException("No faculty ID linked"));
      boolean authorized = session.conductedByFacultyId().equals(facultyId) ||
          facultyAssignmentPort.isAssigned(facultyId, session.subjectId(), session.sectionId());
      if (!authorized) {
          throw new AccessDeniedException("Access denied: You are not assigned to conduct or record attendance for this class");
      }
  }
  ```
  Returns **HTTP 403 Forbidden**.

### Attack Vector 3: JWT Signature Forgery & Tampering
- **Attack:** Attacker changes token payload `"role": "STUDENT"` to `"role": "HOD_ADMIN"` and submits to `/api/v1/academic/departments`.
- **AMCS Defense:** `JwtAuthenticationFilter` invokes `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`.
- If the signature does not match, JJWT throws `SignatureException`, caught by `RestAuthenticationEntryPoint` returning **HTTP 401 Unauthorized** with `code: INVALID_TOKEN`.

### Attack Vector 4: Credential Stuffing & Account Lockout
- **Attack:** Script attempts thousands of common passwords against `/api/v1/auth/login`.
- **AMCS Defense:** Consecutive failed attempts increment `failed_attempts` in `user_accounts`. After 5 failures, `status` becomes `LOCKED` with `locked_until = now() + 15 minutes`. Further login attempts immediately fail with **HTTP 401** (`ACCOUNT_LOCKED`).

---

## 3. Residual Risks & Future Mitigations (Phase 5+)
- **Institutional SSO / SAML / OAuth2:** Integration with institutional Google Workspace or Microsoft Entra ID can be layered over `user_accounts` via OpenID Connect.
- **Refresh Token Invalidation:** Using `token_version` column in `user_accounts` enables instant global logout.
