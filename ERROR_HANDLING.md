# Global Error Handling & Semantic HTTP Mapping

## 1. Unified API Error Model

All REST API error responses return a standardized, consistent JSON envelope:

```json
{
  "timestamp": "2026-08-29T21:55:00.123Z",
  "status": 409,
  "code": "DUPLICATE_ATTENDANCE_RECORD",
  "message": "Student already has an attendance record for this session",
  "path": "/api/v1/sessions/d3b07384-d113-4940-b49d-b4b66df87654/attendance",
  "details": []
}
```

### 1.1 Field Validation Error Details Example
When Bean Validation fails (HTTP 400 Bad Request):
```json
{
  "timestamp": "2026-08-29T21:55:00.123Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed with 2 error(s)",
  "path": "/api/v1/students",
  "details": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Must be a well-formed email address"
    },
    {
      "field": "registrationNumber",
      "rejectedValue": "",
      "message": "Registration number must not be blank"
    }
  ]
}
```

---

## 2. Complete Exception to HTTP Status Mapping

| Source Exception | HTTP Status | Public Code | Description & Client Guidance |
| :--- | :---: | :--- | :--- |
| `HttpMessageNotReadableException` | **400** | `MALFORMED_JSON` | Payload syntax error, missing brackets, or bad date format. |
| `MethodArgumentNotValidException` | **400** | `VALIDATION_FAILED` | `@NotNull`, `@NotBlank`, `@Min`, `@Max`, or regex constraint failure. |
| `MethodArgumentTypeMismatchException` | **400** | `INVALID_PARAMETER_TYPE` | Non-UUID string passed into a UUID path parameter. |
| `ResourceNotFoundException` | **404** | `RESOURCE_NOT_FOUND` | Student, Session, Subject, Period, or Policy ID does not exist. |
| `DuplicateResourceException` | **409** | `DUPLICATE_RESOURCE` | Registration number, subject code, or policy version collision. |
| `ObjectOptimisticLockingFailureException` | **409** | `OPTIMISTIC_LOCK_CONFLICT` | Resource was modified concurrently by another user/tab. Reload and retry. |
| `SessionStateConflictException` | **409** | `SESSION_STATE_CONFLICT` | Session already conducted, cancelled, or rescheduled. |
| `DataIntegrityViolationException` (Unique) | **409** | `DATABASE_CONFLICT` | Concurrency race condition caught by database unique constraint. |
| `AttendanceIntegrityException` | **422** | `INTEGRITY_VIOLATION` | Student unenrolled, session not conducted, or lab group mismatch. |
| `StudentNotEligibleException` | **422** | `STUDENT_NOT_ELIGIBLE` | Student enrollment is not active on the session date. |
| `InvalidSessionTransitionException` | **422** | `INVALID_STATE_TRANSITION` | Illegal transition in session state machine (e.g. Conducted $\to$ Scheduled). |
| `SecurityException` / Unauthorized | **403** | `FORBIDDEN` | Authenticated user lacks permission to conduct or view this session. |
| `Exception` (Unhandled) | **500** | `INTERNAL_SERVER_ERROR` | Sanitized generic internal error. Logged server-side with UUID correlation ID. |

---

## 3. Data Sanitization & Information Leakage Prevention

To ensure enterprise security and zero internal leakage:
1. **No Stack Traces:** Stack traces are logged server-side via SLF4J, never included in response bodies.
2. **No Database Constraint Leaks:** Raw PostgreSQL constraint names (e.g. `uq_attendance_session_student`, `chk_session_conducted_units`) are caught by `GlobalRestExceptionHandler` and mapped to clean, human-readable error messages.
3. **No Hibernate Proxy Exits:** If an entity is not found, an explicit `ResourceNotFoundException` is raised rather than letting Hibernate's `LazyInitializationException` or `EntityNotFoundException` bubble up.
