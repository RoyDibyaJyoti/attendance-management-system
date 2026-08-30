# AMCS Authorization Matrix & Access Control Rules

**Document Version:** 1.0.0  
**Phase:** Phase 4 — Security, Authentication & RBAC  
**Status:** Approved Architecture Plan  

---

## 1. System Roles

| Role Name | Authority String | Description |
| :--- | :--- | :--- |
| **`STUDENT`** | `ROLE_STUDENT` | Enrolled learner. Read-only access strictly bounded to their personal attendance and shortage status. |
| **`FACULTY`** | `ROLE_FACULTY` | Teaching staff. Operational authority restricted to subjects, sections, and sessions to which they are assigned. |
| **`HOD_ADMIN`** | `ROLE_HOD_ADMIN` | Head of Department / System Administrator. Unrestricted institutional authority across academic structures, policies, and overrides. |

---

## 2. Comprehensive Endpoint & Operation Matrix

| Resource / Endpoint | HTTP Method | Permitted Roles | Enforcement Mechanism | Contextual Business Rule / Scope |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | | | | |
| `/api/v1/auth/login` | `POST` | `ANONYMOUS` | Perimeter (`permitAll`) | None. Open public login. |
| `/api/v1/auth/me` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Returns profile of current token subject. |
| `/api/v1/auth/change-password` | `POST` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Verifies current password before change. |
| **Student Profiles** | | | | |
| `/api/v1/students` | `POST` | `HOD_ADMIN` | Perimeter (`hasRole('HOD_ADMIN')`) | Only admin registers students. |
| `/api/v1/students` | `GET` | `HOD_ADMIN`, `FACULTY` | Perimeter (`hasAnyRole(...)`) | List students with pagination. |
| `/api/v1/students/{id}` | `GET` | `STUDENT`, `FACULTY`, `HOD_ADMIN` | Application Service | `STUDENT` can only fetch their own `id`. `FACULTY` and `HOD_ADMIN` can fetch any student. |
| `/api/v1/students/{id}` | `PATCH` | `STUDENT`, `HOD_ADMIN` | Application Service | `STUDENT` can update their own email/contact; `HOD_ADMIN` can update any. |
| **Academic Structure** | | | | |
| `/api/v1/academic/departments` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Public read for authenticated users. |
| `/api/v1/academic/departments` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |
| `/api/v1/academic/periods` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Public read for authenticated users. |
| `/api/v1/academic/periods` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |
| `/api/v1/academic/sections` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Public read for authenticated users. |
| `/api/v1/academic/sections` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |
| `/api/v1/academic/subjects` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Public read for authenticated users. |
| `/api/v1/academic/subjects` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |
| **Faculty Management** | | | | |
| `/api/v1/faculty` | `GET` | `FACULTY`, `HOD_ADMIN` | Application Service (`requireFacultyOrAdmin`) | Faculty directory. |
| `/api/v1/faculty/{id}` | `GET` | `FACULTY`, `HOD_ADMIN` | Application Service (`requireFacultyOrAdmin`) | Faculty profile lookup. |
| `/api/v1/faculty` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin onboarding only. |
| **Enrollments & Lab Groups** | | | | |
| `/api/v1/enrollments` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin enrollment. |
| `/api/v1/enrollments/transfer` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin transfer. |
| `/api/v1/enrollments/students/{id}`| `GET` | `STUDENT`, `FACULTY`, `HOD_ADMIN` | Application Service | `STUDENT` can only view their own enrollments. |
| `/api/v1/lab-groups` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin creates lab cohort divisions. |
| `/api/v1/lab-groups/assign` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin assigns students to lab groups. |
| `/api/v1/lab-groups` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | List lab groups for section. |
| **Timetabling & Sessions** | | | | |
| `/api/v1/sessions` (Create) | `POST` | `FACULTY`, `HOD_ADMIN` | Application Service | `FACULTY` can only create sessions for assigned subjects/sections. `HOD_ADMIN` unrestricted. |
| `/api/v1/sessions/{id}` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | View session timetable. |
| `/api/v1/sessions` (List) | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | List section timetable. |
| `/api/v1/sessions/{id}/cancel` | `POST` | `FACULTY`, `HOD_ADMIN` | Application Service | `FACULTY` must be assigned instructor or conducting faculty. |
| `/api/v1/sessions/{id}/reschedule` | `POST` | `FACULTY`, `HOD_ADMIN` | Application Service | `FACULTY` must be assigned instructor. |
| **Attendance Roll-Call & Sheets** | | | | |
| `/api/v1/sessions/{id}/attendance` (Record) | `POST` | `FACULTY`, `HOD_ADMIN` | Application Service | `FACULTY` can ONLY submit roll-call for their assigned sessions. |
| `/api/v1/sessions/{id}/attendance` (View) | `GET` | `FACULTY`, `HOD_ADMIN` | Application Service | Full class roll-call sheet is restricted to faculty teaching the section or HOD. |
| `/api/v1/attendance/records/{id}/correction` | `POST` | `FACULTY`, `HOD_ADMIN` | Application Service | `FACULTY` must teach the subject; requires reason and approver ID. |
| **Attendance Calculations & Projections** | | | | |
| `/api/v1/students/{id}/attendance/subjects/{subId}` | `GET` | `STUDENT`, `FACULTY`, `HOD_ADMIN` | Application Service | `STUDENT` must match `{id}`. `FACULTY` can view if student is enrolled in their subject. `HOD_ADMIN` unrestricted. |
| `/api/v1/students/{id}/attendance/summary` | `GET` | `STUDENT`, `HOD_ADMIN` | Application Service | `STUDENT` must match `{id}`. `HOD_ADMIN` unrestricted. |
| `/api/v1/students/{id}/attendance/overall` | `GET` | `STUDENT`, `HOD_ADMIN` | Application Service | `STUDENT` must match `{id}`. `HOD_ADMIN` unrestricted. |
| `/api/v1/students/{id}/attendance/subjects/{subId}/shortage` | `GET` | `STUDENT`, `FACULTY`, `HOD_ADMIN` | Application Service | `STUDENT` must match `{id}`. |
| **Policies** | | | | |
| `/api/v1/policies` | `GET` | `ALL_AUTHENTICATED` | Perimeter (`authenticated`) | Institutional policies are public to authenticated users. |
| `/api/v1/policies` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |
| `/api/v1/policies/overall` | `POST` | `HOD_ADMIN` | Application Service (`requireAdminOnly`) | Admin only. |

---

## 3. Concrete Contextual Authorization Rules

### Rule AUTH-STUDENT-01: Self-Attendance Only
```java
if (actor.isStudent()) {
    UUID authenticatedStudentId = actor.studentId()
        .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
    if (!authenticatedStudentId.equals(targetStudentId)) {
        throw new AccessDeniedException("Access denied: You cannot view or modify attendance records of another student");
    }
}
```

### Rule AUTH-FACULTY-01: Teaching Scope Assignment Check
```java
if (actor.isFaculty()) {
    UUID authenticatedFacultyId = actor.facultyId()
        .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));
    boolean isAssigned = facultyAssignmentPort.isAssigned(
        authenticatedFacultyId, session.subjectId(), session.sectionId());
    if (!isAssigned && !session.conductedByFacultyId().equals(authenticatedFacultyId)) {
        throw new AccessDeniedException("Access denied: You are not authorized to conduct or record attendance for this class");
    }
}
```

### Rule AUTH-ADMIN-01: Administrative Omnipresence
If `actor.isAdmin() == true`, the actor bypasses personal ownership checks and is granted full access across all departments, sections, and students, with all administrative actions captured in audit logs.
