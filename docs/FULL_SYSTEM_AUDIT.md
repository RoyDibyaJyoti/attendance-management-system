# AMCS Full System Audit & Implementation Plan

## 1. Current Architecture
The AMCS application currently follows a strict Hexagonal Architecture in the backend (Spring Boot 3, Java 21) and uses React 19 for the frontend.
The database is PostgreSQL 16+, managed via Flyway. Authentication is handled statelessly via JWTs.
Currently, it operates heavily as a "seeded demo" where fundamental relationships (like which faculty teaches which section) are either assumed, hard-coded, or omitted entirely from the domain model to simplify the demonstration. 

## 2. Current Capabilities
The system has highly sophisticated domain logic for attendance calculation, temporal enrollment, and shortage projection. The core calculation engine is robust. However, the foundational administration CRUD workflows, relationships, and user roles (like Class Representative) required to bootstrap and manage this data in a real-world scenario are significantly underdeveloped or missing.

## 3. Requirement Matrix

### Classification Key
- **A** = already fully implemented and usable
- **B** = partially implemented (e.g., POST/GET exist, but no PUT/DELETE)
- **C** = backend exists but frontend/workflow is incomplete
- **D** = frontend exists but backend/domain support is incomplete
- **E** = missing entirely

### HOD/ADMIN
| Feature | Status | Notes |
|---------|--------|-------|
| CRUD students | **B** | `StudentController` has POST/GET/PATCH, missing DELETE/Deactivation. |
| CRUD faculty | **B** | `FacultyController` has POST/GET, missing DELETE/Deactivation. |
| CRUD CR assignments | **E** | CR role does not exist in `UserRole.java`. |
| CRUD departments | **B** | `AcademicStructureController` has POST/GET, missing PUT/DELETE. |
| CRUD sections | **B** | POST/GET only. |
| CRUD subjects | **B** | POST/GET only. |
| Academic periods | **B** | POST/GET only. |
| Faculty-subject-section assignments | **E** | Missing from DB schema (`V1__initial_schema.sql`). |
| Student-section enrollment | **B** | `EnrollmentController` exists, temporal enrollment logic exists, but full lifecycle management is incomplete. |
| Student transfers | **B** | Handled by temporal enrollment, but UI workflow may be rigid. |
| User activation/deactivation | **E** | Missing from backend security model. |
| Credential management | **C** | Password validator exists, but full admin reset flow is incomplete. |
| Timetable management | **E** | No `timetables` table in schema. Sessions are occurrences, not recurring schedules. |
| Attendance policy management | **B** | DB schema has versioned policies, UI exists, but full CRUD lifecycle is incomplete. |
| Session management | **B** | `SessionController` exists, but lacks recurring schedule generation. |
| Attendance corrections | **B** | Audit exists, but authorization flows may lack multi-level approval. |
| Excel import/export | **B** | `ImportController` and staging tables exist. |
| Institutional reports | **A** | `AttendanceReportController` is robust. |
| Dashboards | **B** | Operational, but metrics rely on seeded data assumptions. |
| Search/filtering | **C** | Backend pagination exists, frontend filtering is basic. |

### FACULTY
| Feature | Status | Notes |
|---------|--------|-------|
| Login | **A** | Fully functional. |
| Assigned subjects and sections | **E** | Missing DB relationships connecting Faculty to Subjects/Sections. |
| Timetable | **D** | Frontend has a Timetable page, but backend lacks timetable schema. |
| Create/manage sessions | **B** | Manual creation exists, needs recurring schedule integration. |
| Mark PRESENT/ABSENT/DUTY/MED | **A** | `AttendanceRecords` and domain enums support this. |
| Authorized corrections | **B** | Exists, but needs CR/HOD approval workflow refinement. |
| Attendance history | **B** | Basic session views exist. |
| Student attendance summaries | **B** | Basic views exist. |

### STUDENT
| Feature | Status | Notes |
|---------|--------|-------|
| Login | **A** | Fully functional. |
| Profile | **A** | Implemented. |
| Timetable | **D** | Missing backend support. |
| Subject-wise attendance | **A** | Robust calculation engine. |
| Overall attendance | **A** | Implemented. |
| Shortage calculation | **A** | Predictive forecaster is implemented. |
| Downloadable report | **A** | RPT-001 is implemented. |

### CLASS REPRESENTATIVE (CR)
| Feature | Status | Notes |
|---------|--------|-------|
| Role/permission model | **E** | `CLASS_REPRESENTATIVE` missing from `UserRole.java`. |
| Section-level visibility | **E** | Missing. |
| Attendance modification block | **E** | Missing. |

---

## 4. Database & Domain Gaps
1. **Missing Timetable Schema:** The database needs tables for `recurring_schedules` or `timetables` to generate `sessions`.
2. **Missing Faculty Assignments:** A `faculty_section_assignments` table is needed to link faculty to the sections/subjects they teach.
3. **Missing CR Role:** `UserRole` enum must be updated. A `section_representatives` table is needed to link students to sections as CRs.
4. **Incomplete CRUD Constraints:** Missing cascading deletes or soft-delete (deactivation) flags for master data (departments, sections, users).
5. **Session Generation:** Sessions are currently created manually or assumed to exist. A cron/batch job or trigger is needed to populate sessions from a timetable.

## 5. Backend Gaps
- `AcademicStructureController` lacks PUT, DELETE, and PATCH methods.
- No endpoints to assign faculty to sections.
- No endpoints to manage CRs.
- No timetable management endpoints.
- User management lacks activation/deactivation and admin password resets.

## 6. Frontend Gaps
- Workflows assume a pre-populated database. Starting from an empty database is impossible because you cannot create faculty-section assignments.
- Missing UI for Timetable management (Admin side).
- Missing UI for assigning CRs.
- Missing UI for deactivating users.

## 7. Authentication/Authorization Gaps
- IDOR risks exist if cross-section access isn't strictly enforced for the new CR role.
- No `UserRole.CLASS_REPRESENTATIVE` exists, requiring updates to the JWT claims and Spring Security context.

## 8. Demo-Data Dependencies
- The application relies on seeded data for faculty-section mappings. If a fresh DB is started, a Faculty member logging in will see no sessions and cannot be assigned to any through the UI.

## 9. Recommended Implementation Order (Milestones)

### Milestone 1: Master Data & CRUD Completion
- Implement full PUT/DELETE/Deactivate flows for Departments, Subjects, Sections, Students, and Faculty.
- Add `faculty_section_assignments` DB table and related endpoints.
- Update Admin UI to manage these assignments.
- **Goal:** A fresh database can be fully configured via the UI without SQL seeding.

### Milestone 2: Timetable Engine
- Design and implement Timetable schema (`recurring_schedules`).
- Build Admin UI to create weekly timetables for sections.
- Build backend job/service to project `sessions` from the timetable.
- **Goal:** Faculty see their schedule automatically generated based on assignments.

### Milestone 3: Class Representative (CR) Role
- Add `CLASS_REPRESENTATIVE` to `UserRole`.
- Create `section_representatives` mapping table.
- Build CR Dashboard UI (read-only roster/attendance views).
- Implement security filters to prevent CRs from modifying attendance.
- **Goal:** HOD can elevate a Student to CR for a specific section.

### Milestone 4: Operational Polish & Workflow
- Implement admin password resets and account locking.
- Refine attendance correction workflows to include HOD approval if required.
- Add missing integration tests for the new assignment and timetable flows.
- **Goal:** The system is completely self-sufficient and operational.

## 10. Risk Assessment
- **Data Integrity:** Allowing DELETE on master data (e.g., deleting a Section) could violate foreign keys in `attendance_records` and `sessions`. Soft-deletes (`is_active = false`) must be used exclusively.
- **Concurrency:** The Timetable generator must be idempotent to prevent duplicate sessions being created for the same section/subject/date.
