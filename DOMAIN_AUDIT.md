# Attendance Management and Calculation System (AMCS)
# DOMAIN_AUDIT.md — Phase 1.5 Domain Audit & Hardening Report

**Document Status:** Complete — Phase 1.5 Hardening Baseline  
**Version:** 1.0.0  
**Date:** 2026-08-29  
**Module:** `attendance-domain` (`com.amcs.domain.*`)

---

## 1. Executive Summary

Following the completion of the Phase 1 domain model and calculation engine, a comprehensive domain audit and hardening review (Phase 1.5) was conducted. The objective was to eliminate any hidden assumptions, guarantee data integrity verification, audit mathematical precision, and strictly isolate institutional unknowns before persistence or presentation logic is introduced.

---

## 2. Audit Findings & Corrective Actions

### Finding 1: Hard-Coded Missing Record Treatment
* **Pre-Audit State:** When a conducted session had no `AttendanceRecord` for an enrolled student, the calculation engine silently defaulted the status to `ABSENT`.
* **Risk:** Treating missing data as an unexcused absence is an institutional policy decision, not an immutable law. If a faculty member fails to record attendance, students might be unfairly penalized or disqualified before the omission is noticed.
* **Hardening Fix:**
  * Created `MissingRecordStrategy` enum with three explicit institutional strategies:
    1. `TREAT_AS_ABSENT`: missing records count in denominator and contribute 0 (or ABSENT fraction) to numerator.
    2. `EXCLUDE_FROM_CALCULATION`: the unrecorded session is omitted entirely from the student's calculation.
    3. `MARK_AS_INCOMPLETE`: flags the result as `AttendanceClassification.INCOMPLETE`, preventing definitive shortage/adequate classification until resolved.
  * Added `missingRecordStrategy` and `missingRecordCount` fields to `SubjectAttendanceResult` for full audit transparency.
  * Added `AttendanceClassification.INCOMPLETE` state.

---

### Finding 2: Conflation of Planned vs. Conducted Session Units
* **Pre-Audit State:** `Session` possessed a single `conductedUnits` field ($conductedUnits \ge 1$), which was checked alongside `isCountable()`.
* **Risk:** When a session was scheduled for 3 units on the timetable but cancelled, `conductedUnits` remained 3 on the object even though zero units were actually conducted.
* **Hardening Fix:**
  * Explicitly separated `plannedUnits` (timetable schedule, $\ge 1$) from `conductedUnits` (actual instruction held).
  * Enforced domain constructor invariant:
    * For `SessionStatus.CONDUCTED`: $conductedUnits \ge 1$.
    * For `CANCELLED`, `SCHEDULED`, and `RESCHEDULED`: $conductedUnits$ is strictly $0$.
  * Maintained backward-compatible constructor while guaranteeing that a cancelled 3-unit lab always contributes 0 conducted units to denominator.

---

### Finding 3: Silent Tolerance of Data Corruption & Invariant Violations
* **Pre-Audit State:** Invalid records (e.g. attendance for a cancelled session or for an unenrolled student) were filtered out during calculation, but never reported as corruption to administrators.
* **Risk:** Data corruption in upstream spreadsheets or faculty entry could go unnoticed, hiding bugs or administrative errors.
* **Hardening Fix:**
  * Created `AttendanceIntegrityValidator` providing detection across seven explicit corruption categories:
    1. `STUDENT_NOT_ENROLLED`: Record exists for date outside student enrollment window.
    2. `WRONG_LAB_GROUP`: Attendance recorded for a session belonging to another lab group.
    3. `DUPLICATE_RECORD`: Multiple records for the same student on the same session.
    4. `SESSION_NOT_CONDUCTED`: Attendance submitted for a `CANCELLED` or `SCHEDULED` session.
    5. `OUTSIDE_ACADEMIC_PERIOD`: Session date falls outside the semester dates.
    6. `SUBJECT_MISMATCH`: Record references a session belonging to another course.
    7. `UNKNOWN_SESSION`: Record references a non-existent session ID.
  * Produces an immutable `AttendanceIntegrityReport`.
  * Added `AttendanceIntegrityException` and `calculateSubjectAttendanceStrict(...)` in `AttendanceCalculationEngine` for fail-fast integrity enforcement.

---

### Finding 4: Architectural Condonation Boundary
* **Pre-Audit State:** `CondonationPolicy` was linked into `AttendancePolicy`, risking confusion that condonation could silently alter the base percentage calculation.
* **Risk:** Conflating base attendance with condoned attendance creates audit confusion and legal/regulatory liability.
* **Hardening Fix:**
  * Formally decoupled and documented the lifecycle pipeline:
    $$\text{Raw Attendance} \to \text{Normal Attendance Result} \to \text{Shortage Analysis} \to \text{Condonation Policy} \to \text{Final Eligibility / Adjusted Result}$$
  * `SubjectAttendanceCalculator` remains 100% free of condonation logic.
  * Condonation remains strictly blocked pending institutional confirmation of rules RIC-SC-003 through RIC-SC-006.

---

### Finding 5: Policy Immutability & Historical Reproducibility
* **Pre-Audit State:** Policies were designed as records, but explicit immutability guards and historical calculation tests had not been verified against mutation.
* **Hardening Fix:**
  * Verified `statusContributions` uses `Map.copyOf(...)`.
  * Created `PolicyImmutabilityTest`:
    * Confirmed that calling `.put()` or `.clear()` throws `UnsupportedOperationException`.
    * Confirmed that mutating the source map passed to the constructor has no effect.
    * Confirmed historical reproducibility: Evaluating the exact same sessions and records with Policy v1 (75%) produces `ADEQUATE`, while evaluating with Policy v2 (80%) produces `SHORTAGE`, and recomputing with Policy v1 continues to produce the exact historical result.

---

### Finding 6: Mathematical Distinctness of Overall Aggregation Strategies
* **Pre-Audit State:** In `OverallAttendanceCalculator`, `AGGREGATE_HOURS` was sharing the same switch branch as `AGGREGATE_UNITS`.
* **Risk:** The two strategies were numerically identical in code, which could mask institutional divergence where laboratory units represent multiple clock hours.
* **Hardening Fix:**
  * Separated `AGGREGATE_HOURS` into a dedicated mathematical calculation accepting a `hoursPerUnitBySubject` map (defaulting to 1.0 hr/unit).
  * Formula:
    $$\text{Overall}_{\text{Hours}} = \frac{\sum (A_s \times H_s)}{\sum (C_s \times H_s)} \times 100$$
  * Verified all four strategies on the same dataset produce strictly different percentages:
    * `ARITHMETIC_MEAN`: **70.00%**
    * `AGGREGATE_UNITS`: **63.33%**
    * `AGGREGATE_HOURS`: **74.00%**
    * `WEIGHTED_BY_CREDITS`: **76.67%**
  * Formally documented the two institutional options for how integrated courses enter overall aggregation (component-level entry vs. consolidated entry).

---

### Finding 7: Mathematical Audit & Extreme Boundaries
* **Hardening Fix:**
  * Created `MathematicalAuditTest`:
    * Tested 100,000 unit sessions: Scaled with exact integer precision without overflow.
    * Tested repeating decimal fractions ($1/3 = 0.3333333333$): Evaluated safely without precision loss.
    * Tested $T = 0\%$ boundary: Exactly 0 units required, all remaining can be missed.
    * Tested $T = 100\%$ boundary: Single absence renders target mathematically unreachable ($x_{\min} = -1$).
    * Tested exact threshold boundary: $P = T$ yields shortage = 0.00, surplus = 0.00, and $x_{\min} = 0$.
    * Tested policy validation: Negative thresholds and thresholds $> 100\%$ throw `IllegalArgumentException`.

---

## 3. Test Suite Expansion

| Test Suite | Pre-Audit Count | Post-Audit Count | Added Coverage |
|---|:---:|:---:|---|
| `SubjectAttendanceCalculatorTest` | 22 | 26 | Missing record strategies (`TREAT_AS_ABSENT`, `EXCLUDE_FROM_CALCULATION`, `MARK_AS_INCOMPLETE`), planned vs conducted units, cancellation zero-contribution. |
| `AttendanceIntegrityValidatorTest` | 0 | 9 | All 7 data integrity violations + clean report + fail-fast exception. |
| `PolicyImmutabilityTest` | 0 | 3 | Defensive copy immutability, source map isolation, historical version reproducibility. |
| `OverallAttendanceCalculatorTest` | 9 | 10 | Mathematical distinctness proof of all 4 aggregation strategies on identical data. |
| `MathematicalAuditTest` | 0 | 7 | 100,000 units, periodic repeating fractions, $T=0\%$, $T=100\%$, exact threshold boundaries. |
| Other Existing Suites | 54 | 54 | Shortage math, prediction, integrated subjects, domain invariants, facade. |
| **Total** | **85** | **109** | **+24 high-value domain invariant tests** |

---

## 4. Remaining Institutional Confirmations (RICs Summary)

The domain is fully decoupled and configurable for all remaining institutional decisions:
1. **[RIC-MR-001] Missing Record Policy:** Default institutional strategy for unrecorded sessions (`TREAT_AS_ABSENT` vs. `EXCLUDE_FROM_CALCULATION` vs. `MARK_AS_INCOMPLETE`).
2. **[RIC-S-002 & S-003] Session Units:** Confirmation of standard unit weight per theory period and laboratory block.
3. **[RIC-AS-002] Attendance Status Weights:** Official contribution fractions for `DUTY_LEAVE`, `MEDICAL_LEAVE`, and `ON_DUTY`.
4. **[RIC-TL-002] Integrated Course Combination:** Selection of composite rule (`SEPARATE_THRESHOLDS`, `COMBINED_ALL_SESSIONS`, or `WEIGHTED_AVERAGE`).
5. **[RIC-OA-002 & OA-004] Overall Aggregation & Integrated Participation:** Aggregation strategy selection and whether integrated components enter overall calculation individually or as consolidated subjects.
6. **[RIC-SC-003 to 006] Condonation Rules:** Exact conditions, allowances, and adjustment mechanisms for shortage condonation.

---

## 5. Known Future Extension Points

* **Phase 2 (Persistence & Repositories):** Map immutable domain records to persistence entities while preserving the pure domain calculation engine.
* **Phase 3 (Service Layer & Auditing):** Wire `AttendanceIntegrityValidator` into the batch attendance recording flow to flag corrupt imports before persistence.
* **Phase 4 (Condonation Subsystem):** Implement `CondonationService` downstream of `SubjectAttendanceResult` once institutional rules are codified.
