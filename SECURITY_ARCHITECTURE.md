# AMCS Security Architecture & Integration Blueprint

**Document Version:** 1.0.0  
**Phase:** Phase 4 — Security, Authentication & RBAC  
**Status:** Architecture Review Approved for Implementation  

---

## 1. Executive Summary & Design Principles

The Attendance Management and Calculation System (AMCS) operates in an institutional academic setting where attendance data impacts academic eligibility, graduation qualification, and accreditation audits. Security must guarantee:
1. **Confidentiality:** Students can only view their own attendance records.
2. **Integrity:** Faculty can only mark and correct attendance for classes they are officially assigned to teach.
3. **Accountability:** Every administrative modification and roll-call correction is tied to an authenticated actor.
4. **Hexagonal Purity:** Pure domain calculation logic (`com.amcs.domain.*`) remains **100% free** of Spring Security, JWT, or servlet dependencies.
5. **Decoupled Application Logic:** Application services obtain the authenticated caller through an outbound port abstraction (`CurrentUserPort`), never coupling to `SecurityContextHolder`.

---

## 2. End-to-End Security Architecture Flow

```
[ HTTP Client (Browser / Mobile / Postman) ]
                     │
                     ▼ Bearer <JWT>
┌─────────────────────────────────────────────────────────────┐
│  Spring Security Filter Chain (Infrastructure/Web)         │
│  1. JwtAuthenticationFilter                                 │
│     - Extracts "Authorization: Bearer <token>"              │
│     - Validates signature, expiration, issuer               │
│     - Constructs AuthenticatedPrincipal                     │
│     - Injects into SecurityContextHolder                    │
│  2. SecurityFilterChain (URL-level coarse RBAC)            │
│     - /api/v1/auth/**            → PermitAll                │
│     - /api/v1/academic/**        → hasRole('HOD_ADMIN')     │
│     - /api/v1/sessions/**        → hasAnyRole('FACULTY', 'HOD_ADMIN') │
│     - /api/v1/students/**        → Authenticated            │
│  3. Custom AuthenticationEntryPoint (401 ApiErrorResponse)  │
│  4. Custom AccessDeniedHandler (403 ApiErrorResponse)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Dispatches to Controller
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  REST Controllers (com.amcs.infrastructure.web)             │
│  - Receives request DTOs                                    │
│  - Coarse @PreAuthorize checks if needed                    │
│  - Delegates to Application Services                        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Invokes Use Case
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Application Layer (com.amcs.application)                   │
│  - Application Services (@Transactional)                    │
│  - Queries CurrentUserPort to get AuthenticatedActor         │
│  - Enforces FINE-GRAINED Business Authorization:            │
│    * Student ID ownership check                             │
│    * Faculty teaching assignment check                      │
│    * Session conductor verification                         │
│  - Coordinates repository ports & domain engines            │
└──────────────┬──────────────────────────────┬───────────────┘
               │ Invokes Pure Logic           │ Uses Port
               ▼                              ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│ Pure Domain Layer            │ │ Infrastructure Security    │
│ (com.amcs.domain.*)          │ │ - CurrentUserAdapter       │
│ - Zero Security Dependencies │ │ - Reads SecurityContext    │
│ - Pure Mathematical Engine   │ │ - Translates to            │
│ - Pure Invariants Validator  │ │   AuthenticatedActor       │
└──────────────────────────────┘ └────────────────────────────┘
```

---

## 3. Layer Separation & Hexagonal Boundaries

### Domain Purity Invariant
Under no circumstances may annotations such as `@Secured`, `@PreAuthorize`, `@RolesAllowed`, or classes from `org.springframework.security.*` be imported into `com.amcs.domain.*`. The domain layer calculates attendance from facts; it has no concept of HTTP headers or JSON Web Tokens.

### Application Layer Port Abstraction
Application services must not touch `SecurityContextHolder`. Instead, an outbound port interface is defined in the application layer:

```java
package com.amcs.application.port.out.security;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserPort {
    Optional<AuthenticatedActor> getCurrentActor();
    AuthenticatedActor requireCurrentActor();
}
```

The value object `AuthenticatedActor` contains only domain and application-relevant identifiers:
```java
package com.amcs.application.port.out.security;

import java.util.Optional;
import java.util.UUID;

public record AuthenticatedActor(
    UUID userId,
    String username,
    SecurityRole role,
    Optional<UUID> studentId,
    Optional<UUID> facultyId
) {
    public boolean isStudent() { return role == SecurityRole.STUDENT; }
    public boolean isFaculty() { return role == SecurityRole.FACULTY; }
    public boolean isAdmin() { return role == SecurityRole.HOD_ADMIN; }
}
```

The infrastructure layer implements this port:
```java
package com.amcs.infrastructure.security;

import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentUserAdapter implements CurrentUserPort {
    @Override
    public Optional<AuthenticatedActor> getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SecurityUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal.toActor());
    }

    @Override
    public AuthenticatedActor requireCurrentActor() {
        return getCurrentActor().orElseThrow(() -> 
            new AccessDeniedException("No authenticated actor present in security context"));
    }
}
```

---

## 4. Two-Tier Authorization Architecture

### Tier 1: Perimeter RBAC (Spring Security Filter / Controller)
- Evaluates purely whether the caller's role is syntactically permitted to reach the URL path.
- Returns **HTTP 401 Unauthorized** if no token or token is invalid/expired.
- Returns **HTTP 403 Forbidden** if caller's role is not in the allowed list for the route.

### Tier 2: Core Business Authorization (Application Services)
- Evaluates contextual entity ownership and institutional relationships:
  1. **Student Ownership Rule:** When a caller has `ROLE_STUDENT`, any request to view attendance for `studentId` is checked against `actor.studentId()`. If `!actor.studentId().get().equals(studentId)`, an `AccessDeniedException` is thrown $\to$ **HTTP 403 Forbidden**.
  2. **Faculty Scope Rule:** When a caller has `ROLE_FACULTY`, before creating a session or recording attendance, the service checks whether the faculty member is officially assigned to the `(subjectId, sectionId, academicPeriodId)` via `FacultyAssignmentRepositoryPort`. If unassigned, an `AccessDeniedException` is thrown $\to$ **HTTP 403 Forbidden**.
  3. **Conductor Verification Rule:** A faculty member can only mark roll-call or cancel sessions that they were designated to conduct, unless overridden by `HOD_ADMIN`.

---

## 5. Security Component Inventory

| Package | Component | Responsibility |
| :--- | :--- | :--- |
| `com.amcs.application.port.out.security` | `CurrentUserPort` | Outbound port providing current actor context. |
| `com.amcs.application.port.out.security` | `AuthenticatedActor` | Pure Java record representing authenticated identity and person linkage. |
| `com.amcs.application.port.out.security` | `SecurityRole` | Enum: `STUDENT`, `FACULTY`, `HOD_ADMIN`. |
| `com.amcs.infrastructure.security.jwt` | `JwtTokenProvider` | Generates, parses, and validates signed JWT tokens. |
| `com.amcs.infrastructure.security.jwt` | `JwtAuthenticationFilter` | Filter extracting and validating Bearer tokens on incoming requests. |
| `com.amcs.infrastructure.security.config`| `SecurityConfig` | Configures `SecurityFilterChain`, CORS, CSRF (disabled for stateless API), session management (STATELESS). |
| `com.amcs.infrastructure.security.error` | `RestAuthenticationEntryPoint` | Formats 401 errors into `ApiErrorResponse`. |
| `com.amcs.infrastructure.security.error` | `RestAccessDeniedHandler` | Formats 403 errors into `ApiErrorResponse`. |
| `com.amcs.infrastructure.security.service`| `CustomUserDetailsService` | Loads `UserAccountEntity` by username/email for authentication. |
| `com.amcs.infrastructure.persistence.entity`| `UserAccountEntity` | JPA entity storing credentials, role, status, and person foreign keys. |
| `com.amcs.infrastructure.persistence.entity`| `FacultyAssignmentEntity` | JPA entity tracking faculty assignments to subjects and sections. |
