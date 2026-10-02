# Milestone 1: Master Data CRUD + Faculty Assignments

## Overview
This milestone evolved the AMCS platform from a static seeded demo to a genuinely persistent and configurable application. It transformed the master data modules to support lifecycle soft-deletes and established robust temporal invariants for faculty teaching assignments.

## Accomplishments

### 1. Master Data Lifecycle Management (Soft-Deletes)
- **Database Schema:** Introduced the `is_active` boolean flag across all foundational domain tables (departments, faculty, students, sections, subjects).
- **Domain & Application Services:** Integrated soft-delete checks into entity models and update requests. Master data entities can now be deactivated rather than deleted, preserving historical references (e.g., historical attendance records linked to a deactivated student).
- **Admin UI:** Added status lifecycle toggles to the Student Management portal. Built a comprehensive filtering pattern where Admin dashboards can choose to `includeInactive` resources, whereas Faculty and Student portals exclusively receive active data.

### 2. Temporal Faculty Assignments
- **Database Constraints:** 
  - Designed the new `faculty_section_assignments` table with `assignment_start` and `assignment_end` bounds.
  - Implemented an `EXCLUDE USING gist` constraint utilizing PostgreSQL's `btree_gist` extension to enforce strict non-overlapping temporal constraints at the database level.
- **Application Logic:** 
  - Built `FacultyAssignmentApplicationService` to validate temporal overlaps and manage assignment lifecycle (e.g., terminating current assignments).
  - Ensured historical assignments remain untouched when creating sequential assignments (e.g., Faculty A teaches Aug-Oct, Faculty B teaches Oct-Dec).
- **Faculty UI:** Replaced hardcoded seeded assignments on the Faculty Timetable dashboard with dynamically fetched active assignments using `facultyAssignmentApi.ts`.
- **Admin UI:** Created the `AdminFacultyAssignmentsPage` to empower the HOD/ADMIN to manage temporal teaching contexts dynamically.

### 3. Comprehensive Backend Testing
- Refactored `FacultyAssignmentApplicationServiceTest` and integrated all domain models to respect the new lifecycle fields.
- Verified temporal exclusion logic, database migration integrity (V6 Flyway), and application boundary rules ensuring that overlapping periods are systematically blocked.
- Test Suite passed successfully (`Tests run: 574, Failures: 0, Errors: 0, Skipped: 0`).

## Next Steps
- **Milestone 2:** Proceed with advanced roles (Class Representative - CR), Timetable scheduling constraints, and granular Role-Based Access Control (RBAC).
- Complete End-to-End manual/automation QA to confirm UI synchronization across all modified portals.
