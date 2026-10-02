# Milestone 2 Plan: Timetable, Recurring Classes & Session Generation

## 1. CURRENT STATE
Based on a thorough audit of the `develop/amcs-full-system` branch:
- **What exists:** 
  - `SessionEntity` records individual class occurrences for a specific `session_date` (`LocalDate`). It currently lacks precise time bounds (`start_time`/`end_time`).
  - Faculty can manually create standalone sessions via `FacultyTimetablePage.tsx` and `SessionController.createSession`.
  - `FacultyAssignmentEntity` (from M1) robustly maps Faculty -> Subject + Section over a temporal date range (`assignment_start` to `assignment_end`).
  - `AcademicPeriodEntity` defines the `startDate` and `endDate` of a semester.
- **What is missing:** 
  - There is absolutely no `Timetable` or recurring schedule domain model.
  - There is no automated session generation.
  - `SessionEntity` lacks time-of-day fields, making exact time overlap detection impossible at the session occurrence level.

## 2. M2 FUNCTIONAL REQUIREMENTS
### A. Timetable Definition
- HOD/Admin defines a `TimetableEntry` for a specific `section_id` and `academic_period_id`.
- Configures `subject_id`, `day_of_week` (1-7), `start_time`, `end_time`, `session_type` (THEORY/LAB), and `planned_units`.
- Follows an active/inactive lifecycle for safe deactivation.

### B. Recurring Schedule
- The timetable represents a weekly recurring template bounded by the `AcademicPeriod`.
- Changes to a timetable entry only affect *future* ungenerated sessions.

### C. Session Generation
- An administrative action (or batch job) generates actual `Session` occurrences from the timetable for a requested date range.
- The system dynamically looks up the authoritative `faculty_section_assignments` active on each specific date to resolve the `conducted_by_faculty_id`.
- Ensures idempotency: duplicate generation runs for the same date range will safely ignore already-generated sessions.

### D. Session Lifecycle
- Generated sessions are created with `SCHEDULED` status.
- They seamlessly integrate into the existing M1 lifecycle (conducted, cancelled, rescheduled).

### E. Faculty & Student Workflow
- **Faculty:** Sees only sessions for which they are the resolved instructor on that date. Continues to use the existing Roll-Call workflow.
- **Student:** Sees the schedule dynamically appear on their dashboard based on their active enrollment.

### F. Admin Workflow
- Manages the weekly grid (Timetable) per Section.
- Executes "Generate Sessions" safely without fear of duplication.

## 3. DATA MODEL
### New Entity: `TimetableEntry`
We will introduce a `timetable_entries` table. It does *not* link to a faculty member. Faculty resolution is deferred to generation time via the M1 assignment model.
- `id` (UUID, PK)
- `section_id` (UUID, FK)
- `subject_id` (UUID, FK)
- `academic_period_id` (UUID, FK)
- `day_of_week` (INT, 1=Monday...7=Sunday)
- `start_time` (TIME)
- `end_time` (TIME)
- `session_type` (VARCHAR)
- `planned_units` (INT)
- `is_active` (BOOLEAN)

### Extensions to Existing `SessionEntity`
To support generation and time-based collision detection:
- `timetable_entry_id` (UUID, FK, Nullable) - Links the generated occurrence back to its recurring template. Nullable to support ad-hoc/makeup sessions.
- `start_time` (TIME, Nullable for legacy)
- `end_time` (TIME, Nullable for legacy)

## 4. TEMPORAL VALIDATION
Validation occurs at two distinct layers:

1. **Template Layer (Timetable Entries):**
   - *Invariant:* A section cannot have two subjects scheduled at the same time.
   - *Enforcement:* PostgreSQL `EXCLUDE USING gist` on `timetable_entries` preventing time overlaps for the same `section_id`, `academic_period_id`, and `day_of_week`.

2. **Occurrence Layer (Session Generation):**
   - *Invariant:* A faculty member cannot teach two different sessions at the same exact time.
   - *Enforcement:* Because faculty assignments change over time, this cannot be enforced purely at the `timetable_entries` table. It must be enforced on the `sessions` table using an `EXCLUDE USING gist` constraint on `conducted_by_faculty_id`, `session_date`, and the `[start_time, end_time]` range.
   - *Academic Period Bounds:* Session generation strictly limits `session_date` to fall within the `academic_period` boundaries.

## 5. SESSION GENERATION ALGORITHM
The `SessionGenerationService` algorithm:
1. **Input:** `academicPeriodId`, `startDate`, `endDate`. (Clamped to the `AcademicPeriod` bounds).
2. For each date in the range:
   - Identify the `day_of_week`.
   - Query all active `timetable_entries` for this period matching the day.
   - For each matching entry:
     - **Idempotency Check:** Query `sessions` table for `timetable_entry_id = entry.id` AND `session_date = date`. If exists, skip.
     - **Faculty Resolution:** Query `faculty_section_assignments` for `subject_id`, `section_id` where `assignment_start <= date` and `(assignment_end >= date OR null)`.
     - **Validation:** If no active faculty assignment exists for that exact date, the generation for that specific session is **skipped** (and logged), enforcing the rule that PostgreSQL assignments are the single source of truth.
     - **Creation:** Insert new `SessionEntity` in `SCHEDULED` status.

## 6. API DESIGN
**Admin Timetable Management:**
- `POST /api/v1/timetables` (Role: HOD_ADMIN) - Create a timetable entry.
- `GET /api/v1/timetables?sectionId={id}` (Role: HOD_ADMIN) - List entries.
- `PATCH /api/v1/timetables/{id}/status` (Role: HOD_ADMIN) - Activate/Deactivate entry.

**Session Generation:**
- `POST /api/v1/sessions/generate` (Role: HOD_ADMIN) 
  - Payload: `academicPeriodId`, `startDate`, `endDate`.
  - Purpose: Triggers the idempotent generation algorithm.

## 7. AUTHORIZATION / IDOR
- **Admin:** `ApplicationAuthorizationService.requireAdminOnly` applied to all `/api/v1/timetables` and `/generate` endpoints.
- **Faculty:** Existing rules safely limit faculty to viewing and recording attendance only for `SessionEntity` records where `conducted_by_faculty_id` matches their ID.
- **Students:** Existing rules limit visibility to `SessionEntity` records joined through their active `Enrollment`.

## 8. FRONTEND
- **Admin UI:** Add a new `AdminTimetablePage.tsx` featuring a weekly calendar grid view. Admins select a Section and plot Subject blocks. Includes a "Generate Sessions" action modal.
- **Faculty UI:** Enhance `FacultyTimetablePage.tsx` to display `start_time` and `end_time` for sessions, shifting away from the legacy "create ad-hoc session" as the primary workflow.

## 9. MIGRATION PLAN
**V7__timetable_and_session_times.sql**
1. Create `timetable_entries` table with appropriate foreign keys and constraints.
2. Add `timetable_entry_id`, `start_time`, `end_time` columns to `sessions` table.
3. Add GIST exclusion constraint on `timetable_entries` preventing overlapping times for a section.
4. Add GIST exclusion constraint on `sessions` preventing overlapping times for a faculty member on a specific date.

## 10. SEED DATA
Existing seeded demo sessions will remain untouched (they will simply have `null` `start_time` and `timetable_entry_id`). To demonstrate M2 effectively, the `DatabaseSeeder` should be updated to create sample `TimetableEntry` records and trigger the generation service programmatically during initialization.

## 11. TEST PLAN
- **Integration Tests:** 
  - `TimetableApplicationServiceTest`: Verify overlap prevention, academic period boundary checks.
  - `SessionGenerationServiceTest`: Assert idempotency (running twice produces no duplicates), assert faculty resolution accurately picks up mid-semester assignment handovers (e.g., Faculty A gets week 1, Faculty B gets week 2).
- **E2E Workflow:** Admin plots CS201 on Monday 9AM -> Admin clicks Generate -> Faculty A logs in and sees the scheduled session on Monday -> Faculty A records roll-call.

## 12. FAILURE / EDGE CASES
- **No Faculty Assigned:** The generation algorithm strictly skips generating the session for that date. The Admin is warned in the generation response summary.
- **Mid-Semester Handover:** Handled elegantly. If Faculty A teaches until Oct 15, and Faculty B from Oct 16, the generator naturally binds the session to Faculty A or B based on the exact `session_date`.
- **Holidays:** Generated sessions falling on holidays can be manually cancelled by Admin using the existing `CANCELLED` lifecycle state, which correctly sets `conductedUnits = 0`.

## 13. IMPLEMENTATION ORDER
- **M2.1 Data Model & Migrations:** Implement V7 migration, `TimetableEntry`, and update `SessionEntity`.
- **M2.2 Backend CRUD:** Implement `TimetableApplicationService` and REST endpoints.
- **M2.3 Session Generation:** Implement the `SessionGenerationService` algorithm and validation limits.
- **M2.4 Frontend Timetable Management:** Build `AdminTimetablePage`.
- **M2.5 Integration Testing:** Ensure overlap constraints and generation idempotency are foolproof.

## 14. RISKS
- **Timezone Complexity:** Using `TIME` without timezone might cause edge cases if the institution operates across timezones. *Mitigation:* Document that the system assumes a single institutional timezone.
- **Legacy Session Overlaps:** Legacy sessions lack `start_time`. *Mitigation:* The `EXCLUDE` constraint on `sessions` must use `COALESCE` or only evaluate where times are not null, preventing migration failures on seeded data.

## 15. ACCEPTANCE CRITERIA
1. Admin can configure a non-overlapping weekly timetable for a section.
2. Generating sessions for a date range successfully resolves the correct faculty member assigned on that specific date.
3. Running generation twice for the same dates produces exactly 0 duplicate sessions.
4. Faculty and Student portals correctly display the generated sessions.
5. All backend temporal logic is backed by PostgreSQL GIST exclusion constraints.
