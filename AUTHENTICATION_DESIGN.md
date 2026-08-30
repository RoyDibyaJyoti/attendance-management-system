# AMCS Authentication Design & JWT Specification

**Document Version:** 1.0.0  
**Phase:** Phase 4 — Security, Authentication & RBAC  
**Status:** Approved Architecture Plan  

---

## 1. Decoupled Identity Model

### Separation of Identity and Institutional Person
A core architectural requirement is that authentication accounts (`user_accounts`) are strictly decoupled from academic domain persons (`students`, `faculty`).

```
                    ┌─────────────────────────┐
                    │      user_accounts      │
                    ├─────────────────────────┤
                    │ id (UUID, PK)           │
                    │ username (VARCHAR)      │
                    │ email (VARCHAR)         │
                    │ password_hash (VARCHAR) │
                    │ role (VARCHAR)          │
                    │ student_id (UUID, NULL) │
                    │ faculty_id (UUID, NULL) │
                    │ status (VARCHAR)        │
                    │ failed_attempts (INT)   │
                    │ locked_until (TIMESTAMP)│
                    │ created_at (TIMESTAMP)  │
                    │ updated_at (TIMESTAMP)  │
                    └───────────┬─────────────┘
                                │
               ┌────────────────┴────────────────┐
               │ (1:1 optional)                  │ (1:1 optional)
               ▼                                 ▼
    ┌────────────────────┐            ┌────────────────────┐
    │      students      │            │      faculty       │
    ├────────────────────┤            ├────────────────────┤
    │ id (UUID, PK)      │            │ id (UUID, PK)      │
    │ reg_number (UNIQUE)│            │ employee_id(UNIQUE)│
    │ name               │            │ name               │
    │ email (UNIQUE)     │            │ email (UNIQUE)     │
    │ department_id      │            │ department_id      │
    └────────────────────┘            └────────────────────┘
```

### Key Invariants
1. **Single Account per Person:** A student or faculty member can have at most one associated `user_accounts` row (`uq_user_student` and `uq_user_faculty` unique constraints with partial indexes where not null).
2. **Mutually Exclusive Persona:**
   - If `role = 'STUDENT'`: `student_id IS NOT NULL` AND `faculty_id IS NULL`.
   - If `role = 'FACULTY'`: `faculty_id IS NOT NULL` AND `student_id IS NULL`.
   - If `role = 'HOD_ADMIN'`: `student_id IS NULL`. (`faculty_id` may be linked if an HOD is an active professor in the institution).
3. **Login Identifier Flexibility:** Users can log in with either their `username` (e.g. `CS2026-001` or `EMP-8802`) or their registered `email`.

---

## 2. Password Security & Hashing

### Algorithm
AMCS uses the modern adaptive password hashing standard:
- **Primary:** `BCryptPasswordEncoder(strength = 12)` or `Argon2PasswordEncoder(16, 32, 1, 65536, 3)` via Spring Security's `PasswordEncoderFactories.createDelegatingPasswordEncoder()`.
- Default prefix `{bcrypt}` enables future transparent rehashing without database rewrites if algorithm upgrades occur.

### Password Invariants
- **Never Plaintext:** Plaintext passwords are never stored, never logged, and never included in JWT claims.
- **Complexity Requirements:** Minimum 8 characters, maximum 128 characters, containing at least one uppercase letter, one lowercase letter, one digit, and one symbol.
- **Response Hygiene:** `password_hash` is marked `@JsonIgnore` and never exposed in REST responses or DTOs.

---

## 3. JWT Access Token Specification

### Token Standard
- **Format:** Signed JSON Web Token (RFC 7519)
- **Signature Algorithm:** HMAC-SHA256 (`HS256`) using a cryptographically secure 256-bit key (minimum 32 bytes) configured via `AMCS_JWT_SECRET`.
- **Token Lifetime:** Default 60 minutes (3600 seconds) for access tokens.

### Token Claims Structure

```json
{
  "iss": "amcs-auth-service",
  "sub": "b2f6f4d2-7c38-4e89-9a21-789a42f568e1",
  "username": "CS2026-001",
  "email": "alice@univ.edu",
  "role": "STUDENT",
  "studentId": "d3b07384-d113-4f9e-9d22-123456789abc",
  "facultyId": null,
  "iat": 1756500000,
  "exp": 1756503600
}
```

### Claim Definitions

| Claim | Type | Description |
| :--- | :--- | :--- |
| `iss` | String | Issuer identifier (`amcs-auth-service`). |
| `sub` | String (UUID) | User account ID (`user_accounts.id`). |
| `username` | String | User's unique login handle. |
| `email` | String | User's email address. |
| `role` | String | Security role: `STUDENT`, `FACULTY`, or `HOD_ADMIN`. |
| `studentId` | String (UUID) | Linked student ID (populated only when role is `STUDENT`). |
| `facultyId` | String (UUID) | Linked faculty ID (populated only when role is `FACULTY` or faculty HOD). |
| `iat` | Long | Epoch timestamp of token creation. |
| `exp` | Long | Epoch timestamp of token expiration. |

### Token Validation Rules
1. **Signature Verification:** Token must be signed by the active server secret key.
2. **Expiration Check:** Current time must be before `exp`. Expired tokens are immediately rejected with **HTTP 401** (`TOKEN_EXPIRED`).
3. **Format Integrity:** Malformed or truncated tokens return **HTTP 401** (`MALFORMED_TOKEN`).
4. **Account State:** If token claims reference a deleted or suspended account, the request is rejected.

---

## 4. Account Lifecycle & Status

```
                 [ Registration / Provisioning ]
                               │
                               ▼
    ┌────────────────────► [ ACTIVE ] ◄────────────────────┐
    │                          │                           │
    │ Unlocked after lockout   │ 5 failed attempts         │ Admin reactivation
    │                          ▼                           │
    │                     [ LOCKED ]                       │
    │                          │                           │
    └──────────────────────────┴───────────────┐           │
                                               ▼           │
                                         [ SUSPENDED ] ────┘
                                               │
                                               ▼
                                        [ DEACTIVATED ]
                                        (Terminal state)
```

- **`ACTIVE`:** Account is allowed full access according to its role.
- **`LOCKED`:** Temporary lock caused by consecutive failed password attempts (default 5 attempts, locked for 15 minutes).
- **`SUSPENDED`:** Administrative freeze (e.g. disciplinary action or fee hold).
- **`DEACTIVATED`:** Soft-deleted account for former students or departed faculty. Credentials can no longer be used.

---

## 5. Authentication API Catalog

### 1. User Login
- **Endpoint:** `POST /api/v1/auth/login`
- **Request:**
  ```json
  {
    "usernameOrEmail": "CS2026-001",
    "password": "SecurePassword123!"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "b2f6f4d2-7c38-4e89-9a21-789a42f568e1",
      "username": "CS2026-001",
      "email": "alice@univ.edu",
      "role": "STUDENT",
      "studentId": "d3b07384-d113-4f9e-9d22-123456789abc",
      "facultyId": null
    }
  }
  ```

### 2. Current User Profile
- **Endpoint:** `GET /api/v1/auth/me`
- **Headers:** `Authorization: Bearer <token>`
- **Success Response (200 OK):** Returns user account info and linked student/faculty details.

### 3. Change Password
- **Endpoint:** `POST /api/v1/auth/change-password`
- **Headers:** `Authorization: Bearer <token>`
- **Request:**
  ```json
  {
    "currentPassword": "SecurePassword123!",
    "newPassword": "NewSecurePassword456!"
  }
  ```
- **Success Response (200 OK):** `{ "message": "Password updated successfully" }`

---

## 6. Refresh Token & Logout Extensibility
- In Phase 4, access tokens are stateless JWTs with 60-minute lifetime.
- **Future Extensibility:** The schema reserves a `token_version INT NOT NULL DEFAULT 1` column in `user_accounts`. Incrementing `token_version` on password change or global logout allows instant invalidation of all previously issued tokens without storing token blacklists in memory or Redis.
