# Attendance Management and Calculation System
# DOMAIN_RULES.md — Phase 0

**Document Status:** Draft — Pending Institutional Input  
**Version:** 0.1.0  
**Date:** 2026-08-29  
**Classification:** Internal — Domain Analysis

---

## Purpose of This Document

This document catalogues every attendance-related rule that the system must implement. Each rule is explicitly classified:

- **[CONFIRMED]** — The rule is unambiguous and universally applicable.
- **[ASSUMPTION]** — A reasonable technical assumption made to enable analysis; subject to reversal by institutional policy.
- **[REQUIRES INSTITUTIONAL CONFIRMATION — RIC]** — The rule is institution-specific and MUST be answered before Phase 1 implementation begins.

Rules classified as ASSUMPTION are explicitly documented so they can be overridden without rearchitecting the system. Rules classified as RIC represent open questions; placeholder logic MUST NOT be implemented for these.

---

## Table of Contents

1. [Session and Unit Counting Rules](#1-session-and-unit-counting-rules)
2. [Theory Attendance Rules](#2-theory-attendance-rules)
3. [Laboratory Attendance Rules](#3-laboratory-attendance-rules)
4. [Attendance Status Definitions](#4-attendance-status-definitions)
5. [Subject-Level Attendance Calculation Rules](#5-subject-level-attendance-calculation-rules)
6. [Theory-Integrated-Laboratory Subject Rules](#6-theory-integrated-laboratory-subject-rules)
7. [Overall / General Attendance Calculation Rules](#7-overall--general-attendance-calculation-rules)
8. [Shortage and Condonation Rules](#8-shortage-and-condonation-rules)
9. [Future Attendance Prediction Rules](#9-future-attendance-prediction-rules)
10. [Calendar and Scheduling Rules](#10-calendar-and-scheduling-rules)
11. [Reporting Period Rules](#11-reporting-period-rules)
12. [Retroactive and Correction Rules](#12-retroactive-and-correction-rules)
13. [Pre-Implementation Decision Checklist](#13-pre-implementation-decision-checklist)

---

## 1. Session and Unit Counting Rules

### Rule S-001: Unit of Attendance

**[CONFIRMED]** The atomic unit of attendance is a "conducted unit," not a calendar date.  
A session may represent one or more conducted units depending on its type and institutional practice.

### Rule S-002: Theory Session Unit Count

**[ASSUMPTION]** A single theory session (one class period) counts as 1 conducted unit.

> **[RIC-S-002]** REQUIRES INSTITUTIONAL CONFIRMATION: Does the institution use periods/hours as the counting unit for theory classes? Is it always 1 unit per session, or can a single theory slot represent 2 units?

### Rule S-003: Laboratory Session Unit Count

**[RIC-S-003]** REQUIRES INSTITUTIONAL CONFIRMATION:

A laboratory session typically spans multiple periods (e.g., 2 or 3 periods for a 2- or 3-hour lab). The system requires a definitive answer to:

a) Does a single lab session (regardless of hours) count as 1 attendance unit?  
b) Does a 3-hour lab count as 3 units (one per period)?  
c) Does the institution use a fixed "lab session = N units" rule, or is it variable per subject?

**PLACEHOLDER LOGIC IS PROHIBITED here. This directly affects the denominator in lab attendance percentage.**

### Rule S-004: Cancelled Session Denominator Impact

**[CONFIRMED]** A session with status CANCELLED MUST NOT be included in the denominator (total conducted units). The denominator only counts sessions that were actually CONDUCTED.

### Rule S-005: Rescheduled Session Handling

**[ASSUMPTION]** When a session is rescheduled, the original session is marked CANCELLED and a new session is created. Only the new session (if CONDUCTED) contributes to the denominator.

> **[RIC-S-005]** REQUIRES INSTITUTIONAL CONFIRMATION: Should a rescheduled session be treated differently from a new session? Does the reschedule affect how student absences on the original date are treated?

### Rule S-006: Make-up / Compensatory Session Counting

**[RIC-S-006]** REQUIRES INSTITUTIONAL CONFIRMATION:  

When an extra session is conducted (e.g., to compensate for a holiday), does it:
a) Count toward the denominator (and students who miss it are marked absent)?
b) Count toward the denominator only for students who attend (i.e., it is optional)?
c) Not count toward the denominator at all?

---

## 2. Theory Attendance Rules

### Rule T-001: Theory Percentage Calculation

**[CONFIRMED]** Theory attendance percentage = (attended theory units / conducted theory units) × 100.

The "attended" and "conducted" definitions depend on Rules S-002 and the StatusContribution mapping (see Section 4).

### Rule T-002: Theory Threshold

**[RIC-T-002]** REQUIRES INSTITUTIONAL CONFIRMATION:  
What is the minimum required theory attendance percentage?  
Common values: 75%, 80%, 85%. This may vary by department, programme, or subject.

### Rule T-003: Theory-Only Subjects

**[ASSUMPTION]** For subjects with SessionType THEORY only, the theory attendance percentage IS the subject attendance percentage.

---

## 3. Laboratory Attendance Rules

### Rule L-001: Lab Percentage Calculation

**[CONFIRMED]** Lab attendance percentage = (attended lab units / conducted lab units) × 100.  
The definition of "unit" is governed by Rule S-003 (REQUIRES INSTITUTIONAL CONFIRMATION).

### Rule L-002: Lab Threshold

**[RIC-L-002]** REQUIRES INSTITUTIONAL CONFIRMATION:  
What is the minimum required laboratory attendance percentage?  
This may be the same as or different from the theory threshold.  
Some institutions require 100% lab attendance; others use 75% or 80%.

### Rule L-003: Lab-Only Subjects

**[ASSUMPTION]** For subjects with SessionType LAB only, the lab attendance percentage IS the subject attendance percentage.

### Rule L-004: Lab Group Split

**[RIC-L-004]** REQUIRES INSTITUTIONAL CONFIRMATION:  
When a section is split into two groups (A and B) for laboratory sessions with staggered schedules:
a) Are these tracked as separate sub-sections or as separate session streams within the same section?
b) Does each group's lab schedule contribute to all students' denominators, or only the relevant group's?

---

## 4. Attendance Status Definitions

### Rule AS-001: Minimum Required Statuses

**[CONFIRMED]** The system MUST support at minimum: PRESENT and ABSENT.

### Rule AS-002: Additional Status Types

**[RIC-AS-002]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Which of the following additional statuses does the institution use?

| Status | Common Meaning | Counts as Present? |
|---|---|---|
| DUTY_LEAVE (DL) | Official duty (sports, cultural events, competitions) | INSTITUTION SPECIFIC |
| MEDICAL_LEAVE (ML) | Documented medical absence | INSTITUTION SPECIFIC |
| ON_DUTY (OD) | Faculty-sanctioned duty | INSTITUTION SPECIFIC |
| LATE (L) | Present but late | INSTITUTION SPECIFIC |
| HALF_DAY | Present for partial session | INSTITUTION SPECIFIC |

The system will store the status as a code and use the configured StatusContributionMap to determine the fraction contributed to the numerator. This avoids hard-coding "DUTY_LEAVE = present."

### Rule AS-003: Late / Half-Day Contribution

**[RIC-AS-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
If LATE or HALF_DAY statuses are used, what fraction do they contribute to the numerator?
- Full unit (1.0)?
- Half unit (0.5)?
- Zero (0.0)?

### Rule AS-004: Status Retroactive Change

**[ASSUMPTION]** A faculty or authorized admin may change a student's attendance status for a past session (within the correction policy). This generates an audit event and must be approved if the session is locked.

---

## 5. Subject-Level Attendance Calculation Rules

### Rule SL-001: Calculation Basis

**[CONFIRMED]** Subject-level attendance is computed from raw session and attendance records. It is never stored as a manually entered percentage.

### Rule SL-002: Decimal Precision

**[ASSUMPTION]** Attendance percentages are computed to 2 decimal places, rounded using HALF_UP. All intermediate calculations use BigDecimal.

> **[RIC-SL-002]** REQUIRES INSTITUTIONAL CONFIRMATION: What decimal precision does the institution use for attendance percentages? Is rounding always HALF_UP, or does the institution use truncation?

### Rule SL-003: Enrollment Window Enforcement

**[CONFIRMED]** Only sessions conducted within a student's active enrollment window for the subject-section are counted. A student who joins mid-semester MUST NOT have sessions before their enrollment date counted in their denominator.

### Rule SL-004: Missing Attendance Record Handling

**[ASSUMPTION]** If a session is CONDUCTED but a student has no AttendanceRecord (e.g., faculty forgot to mark that student), the system treats it as ABSENT for calculation purposes and generates a data quality warning.

> **[RIC-SL-004]** REQUIRES INSTITUTIONAL CONFIRMATION: Should missing records be treated as ABSENT, or should they be excluded from the calculation until explicitly marked?

### Rule SL-005: Attendance Per Semester vs. Per Term

**[RIC-SL-005]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Is attendance calculated per semester, per term, or over the full academic year?  
Can a student be in shortage in Semester 1 but eligible in Semester 2 independently?

---

## 6. Theory-Integrated-Laboratory Subject Rules

### Rule TL-001: Combined Subject Definition

**[ASSUMPTION]** A THEORY_INTEGRATED_LABORATORY subject has two separate session streams: theory sessions and lab sessions. Each stream may have its own attendance policy.

### Rule TL-002: Combined Attendance Calculation

**[RIC-TL-002]** REQUIRES INSTITUTIONAL CONFIRMATION:  
For a subject with both theory and lab components, how is the overall subject attendance computed?

Options observed in practice:
a) **Separate thresholds:** Theory attendance and lab attendance are reported and checked separately. A student must meet BOTH thresholds independently.
b) **Combined calculation:** All sessions (theory + lab) are pooled. One percentage and one threshold apply.
c) **Weighted combination:** Theory percentage × weight + Lab percentage × (1 - weight) = Subject percentage.

**This is a critical design decision. The system must support all three patterns via policy configuration.**

### Rule TL-003: Integrated Subject Defaulter Logic

**[RIC-TL-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
For shortage purposes in an integrated subject, is a student in shortage if:
a) They fail ANY one component (theory OR lab)?
b) They fail the combined/weighted percentage?

---

## 7. Overall / General Attendance Calculation Rules

### Rule OA-001: Overall Attendance Exists Separately

**[CONFIRMED]** The institution may calculate an "overall" or "general" attendance metric that is separate from individual subject attendance percentages.

### Rule OA-002: Overall Aggregation Method

**[RIC-OA-002]** REQUIRES INSTITUTIONAL CONFIRMATION — HIGHEST PRIORITY:  
How is overall/general attendance computed? Known patterns:

a) **Arithmetic Mean:** Average of all subject attendance percentages.  
   `overall = mean(subject1%, subject2%, ..., subjectN%)`

b) **Credit-Weighted Mean:** Each subject's percentage is weighted by its credit hours.  
   `overall = sum(subject_i% × credits_i) / sum(credits_i)`

c) **Aggregate Hours:** Total attended sessions across ALL subjects / Total conducted sessions across ALL subjects × 100. (This is NOT the mean of percentages; it is computed from raw aggregate counts.)

d) **Custom Formula:** Some institutions use a formula that may additionally factor in lab sessions differently.

**PLACEHOLDER LOGIC IS PROHIBITED. The implementation MUST wait for institutional confirmation of the exact formula.**

### Rule OA-003: Overall Threshold

**[RIC-OA-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
What is the minimum required overall attendance percentage?  
This may be the same as or different from subject-level thresholds.

### Rule OA-004: Subject Exclusions from Overall

**[RIC-OA-004]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Are any subject types excluded from the overall calculation?  
(e.g., are lab sessions, seminars, or electives excluded from overall calculation?)

### Rule OA-005: Overall Threshold Enforcement Scope

**[RIC-OA-005]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Is overall attendance used for:
a) Student display only (informational)?
b) Eligibility determination (exam eligibility, promotion)?
c) Both?

---

## 8. Shortage and Condonation Rules

### Rule SC-001: Shortage Definition

**[CONFIRMED]** A student is in attendance shortage for a subject if their calculated attendance percentage is below the configured minimum threshold.

### Rule SC-002: Shortage Unit Count

**[ASSUMPTION]** The number of "units short" is computed as:  
`units_short = ceil((threshold% × total_conducted_units - attended_units) / (threshold% - 0))`  
Or equivalently: the minimum additional attended units needed to reach the threshold given current conducted units.

> **[RIC-SC-002]** REQUIRES INSTITUTIONAL CONFIRMATION: Should "units short" be expressed as number of periods/sessions, or in a different unit?

### Rule SC-003: Condonation Definition

**[RIC-SC-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Does the institution allow attendance condonation?  
Condonation is the practice of granting a student exemption from a portion of their attendance shortage under specific conditions.

### Rule SC-004: Condonation Trigger Conditions

**[RIC-SC-004]** REQUIRES INSTITUTIONAL CONFIRMATION:  
If condonation is allowed, what triggers it?
a) Medical leave documentation?
b) Duty leave (sports/cultural)?
c) Reaching a specific attendance band (e.g., 65%-74% gets 5% condonation)?
d) Faculty/HOD approval on a case-by-case basis?

### Rule SC-005: Condonation Calculation Method

**[RIC-SC-005]** REQUIRES INSTITUTIONAL CONFIRMATION:  
If condonation is allowed, how is it applied?
a) Add N percentage points to the computed percentage?
b) Add N units to the numerator?
c) Mark specific absence records as condoned, which changes their StatusContribution?

**The implementation approach differs significantly between these methods.**

### Rule SC-006: Maximum Condonation Allowance

**[RIC-SC-006]** REQUIRES INSTITUTIONAL CONFIRMATION:  
What is the maximum condonation that can be applied per student per subject?  
Is there a minimum attendance floor below which condonation cannot be granted?  
(e.g., "Cannot condone below 60% attendance under any circumstance")

### Rule SC-007: Condonation Audit

**[CONFIRMED]** Any condonation applied to a student's attendance record MUST be audited: who approved, when, reason, and the specific records or percentage adjusted.

---

## 9. Future Attendance Prediction Rules

### Rule FP-001: Prediction Purpose

**[CONFIRMED]** The prediction engine exists to inform students: "How many more classes must you attend to reach the threshold?" and "How many more classes can you miss without going into shortage?"

### Rule FP-002: Required Inputs for Prediction

**[ASSUMPTION]** Prediction requires:
- Current attended units (A)
- Current conducted units (C)
- Remaining sessions yet to be conducted (R)
- Threshold percentage (T)

### Rule FP-003: Prediction Formula — Classes Required

**[ASSUMPTION — to be confirmed]**  
Minimum consecutive classes to attend (assuming zero absences hereafter):  
`classes_to_attend = max(0, ceil((T/100 × (C + R) - A))`  
This gives the number of future classes that must be attended (out of R remaining) to hit threshold T.

### Rule FP-004: Prediction Formula — Classes Allowed to Miss

**[ASSUMPTION — to be confirmed]**  
Maximum additional classes a student can miss:  
`classes_can_miss = max(0, floor((A + R - T/100 × (C + R)) / (1 - T/100)))`  
(Assuming all remaining classes are attended; this is the number that can be missed while still meeting T.)

### Rule FP-005: Remaining Sessions Source

**[RIC-FP-005]** REQUIRES INSTITUTIONAL CONFIRMATION:  
How is the "remaining sessions" (R) value determined?
a) From a predefined timetable (fixed total expected sessions)?
b) From a manually entered "expected total sessions" field per subject?
c) Estimated based on remaining calendar weeks × sessions per week from the timetable?
d) Not available (prediction not supported)?

### Rule FP-006: Prediction Accuracy Disclaimer

**[CONFIRMED]** The prediction engine MUST clearly label its output as an estimate. Any change to future session count invalidates the prediction.

---

## 10. Calendar and Scheduling Rules

### Rule CAL-001: Holiday Handling

**[ASSUMPTION]** Sessions cannot be scheduled on official holidays defined in the Academic Calendar. If a session is inadvertently created on a holiday, it should generate a validation warning (not a hard error, to allow edge cases).

### Rule CAL-002: Extra Working Day Sessions

**[ASSUMPTION]** Sessions on extra working days (e.g., a Saturday designated as a working day) are valid and count toward the denominator.

### Rule CAL-003: Cross-Day Sessions

**[RIC-CAL-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Can a session span midnight (start time before midnight, end time after)? This is unlikely in a university context but must be confirmed.

### Rule CAL-004: Academic Calendar Boundaries

**[CONFIRMED]** Sessions must fall within the academic year date range. Sessions outside the academic year boundaries are invalid.

---

## 11. Reporting Period Rules

### Rule RP-001: Period Granularity

**[RIC-RP-001]** REQUIRES INSTITUTIONAL CONFIRMATION:  
At what granularity is attendance reported?
a) Per semester (most common)?
b) Per academic year?
c) Per month (for internal monitoring)?
d) Per custom date range?

### Rule RP-002: Mid-Semester Reporting

**[RIC-RP-002]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Does the institution require an "internal test" or "mid-term" attendance snapshot?  
If so, is attendance computed from semester start to mid-term date, or is it a separate calculation period?

### Rule RP-003: Exam Eligibility Cutoff

**[RIC-RP-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
When is the attendance cutoff date for exam eligibility?  
Is it a fixed date per academic year, or computed (e.g., "last teaching day before exam schedule")?

### Rule RP-004: Report Freeze

**[RIC-RP-004]** REQUIRES INSTITUTIONAL CONFIRMATION:  
After a reporting cutoff date, can attendance records still be modified for that period?  
Who can authorize modifications after the cutoff?

---

## 12. Retroactive and Correction Rules

### Rule RC-001: Correction Window

**[RIC-RC-001]** REQUIRES INSTITUTIONAL CONFIRMATION:  
Within how many days after a session can faculty edit their attendance submission without elevated approval?  
(The "grace period")

### Rule RC-002: Post-Lock Corrections

**[ASSUMPTION]** After the grace period, any correction requires HOD or Department Admin approval and is fully audited with a mandatory reason field.

### Rule RC-003: Retroactive Session Cancellation

**[RIC-RC-003]** REQUIRES INSTITUTIONAL CONFIRMATION:  
If a session already marked CONDUCTED (with attendance) is later cancelled:
a) Are the attendance records for that session soft-deleted?
b) Are they retained for audit but excluded from calculation?
c) Is this operation allowed at all, or does it require Super Admin approval?

**This is a data integrity question with significant implementation implications.**

### Rule RC-004: Policy Change Retroactivity

**[CONFIRMED]** A change to an Attendance Calculation Policy MUST NOT silently alter historical computation results. If a policy change is intended to apply retroactively, it MUST be an explicit, audited action. Reports generated prior to the policy change are not automatically invalidated.

### Rule RC-005: Bulk Correction Workflow

**[ASSUMPTION]** If a bulk import is committed with errors, a correction can be applied via a second import that targets the same sessions with corrected values. The system performs an UPSERT with full audit of what changed.

> **[RIC-RC-005]** REQUIRES INSTITUTIONAL CONFIRMATION: Is a bulk correction via re-import acceptable, or does the institution require record-by-record manual correction?

---

## 13. Pre-Implementation Decision Checklist

The following questions MUST be answered before Phase 1 implementation begins. Phase 1 MUST NOT commence without written confirmation of each item.

### Category 1: Unit Counting

- [ ] **RIC-S-002** How many units does a single theory session count as?
- [ ] **RIC-S-003** How many units does a single lab session count as? (1 attendance event vs. N units for N periods)
- [ ] **RIC-S-006** Do make-up/compensatory sessions count toward the denominator?

### Category 2: Session Types and Subject Classification

- [ ] **RIC** Are there subject types beyond THEORY, LAB, and THEORY_INTEGRATED_LAB?
- [ ] **RIC-L-004** How are lab group splits (Group A / Group B) handled?

### Category 3: Attendance Statuses

- [ ] **RIC-AS-002** Which attendance statuses are in use (DL, ML, OD, LATE, HALF_DAY)?
- [ ] **RIC-AS-002** Does each status count as present, absent, or a fraction?
- [ ] **RIC-AS-003** What fraction does LATE / HALF_DAY contribute to the numerator?

### Category 4: Thresholds

- [ ] **RIC-T-002** What is the theory attendance threshold percentage?
- [ ] **RIC-L-002** What is the lab attendance threshold percentage?
- [ ] **RIC-OA-003** What is the overall attendance threshold percentage?
- [ ] **RIC** Does the threshold vary by department, programme, or subject type?

### Category 5: Theory-Integrated-Lab Subjects

- [ ] **RIC-TL-002** How is attendance calculated for THEORY_INTEGRATED_LAB subjects? (separate thresholds, combined, or weighted?)
- [ ] **RIC-TL-003** For shortage detection, is a student in shortage if they fail theory OR lab, or only if they fail the combined metric?

### Category 6: Overall Attendance

- [ ] **RIC-OA-002** What is the exact formula for overall/general attendance? (arithmetic mean, credit-weighted mean, aggregate hours, or other?)
- [ ] **RIC-OA-004** Are any subjects excluded from the overall calculation?
- [ ] **RIC-OA-005** Is overall attendance used for eligibility determination or informational display only?

### Category 7: Condonation

- [ ] **RIC-SC-003** Does the institution allow condonation?
- [ ] **RIC-SC-004** What triggers condonation eligibility?
- [ ] **RIC-SC-005** How is condonation applied (percentage addition, numerator addition, or record-level change)?
- [ ] **RIC-SC-006** What is the maximum condonation allowance and the minimum attendance floor?

### Category 8: Prediction

- [ ] **RIC-FP-005** How is the "remaining sessions" count for prediction determined?

### Category 9: Decimal Precision and Rounding

- [ ] **RIC-SL-002** To how many decimal places are attendance percentages reported? Is rounding HALF_UP or truncation?

### Category 10: Operational

- [ ] **RIC-RC-001** What is the grace period for faculty to edit submitted attendance?
- [ ] **RIC-RP-001** At what granularity is attendance reported (per semester, year, or custom)?
- [ ] **RIC-RP-003** When is the attendance cutoff date for exam eligibility?
- [ ] **RIC-RC-003** Is retroactive session cancellation allowed, and what happens to existing attendance records?

---

*End of DOMAIN_RULES.md*
