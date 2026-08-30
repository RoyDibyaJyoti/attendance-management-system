# Attendance Management and Calculation System (AMCS)
# POLICY_DESIGN.md — Policy Abstraction & Versioning Architecture

**Document Status:** Complete — Phase 1.5 Hardening Baseline  
**Version:** 1.1.0  
**Date:** 2026-08-29  
**Module:** `attendance-domain` (`com.amcs.domain.policy.*`)

---

## 1. Architectural Motivation

In university attendance management, policies vary across departments, programmes, and course types. Hardcoding institutional assumptions into calculation code creates architectural fragility. The AMCS policy subsystem isolates institutional rules into declarative, versioned policy objects.

---

## 2. Subject-Level Policy (`AttendancePolicy`)

### 2.1 Schema & Immutability
```java
public record AttendancePolicy(
    UUID id,
    String name,
    int version,
    BigDecimal minimumThresholdPercentage,
    Map<AttendanceStatus, BigDecimal> statusContributions,
    MissingRecordStrategy missingRecordStrategy,
    Optional<CondonationPolicy> condonationPolicy,
    Instant effectiveFrom,
    Optional<Instant> effectiveTo
)
```

### 2.2 Status Contribution Strategy
Attendance contribution is modeled as a map:
$$\text{statusContributions}: \text{AttendanceStatus} \to [0.0, 1.0]$$
* Any status omitted from the map safely defaults to `BigDecimal.ZERO` via `getContribution()`.
* Map is encapsulated via `Map.copyOf(...)` and throws `UnsupportedOperationException` on mutation attempts.

### 2.3 Configurable Missing Record Strategy
Rather than hardcoding missing attendance records as absences:
* `TREAT_AS_ABSENT`: Adds conducted units to denominator, adds 0 to numerator.
* `EXCLUDE_FROM_CALCULATION`: Omit session entirely from student calculation.
* `MARK_AS_INCOMPLETE`: Flags classification as `INCOMPLETE` until administrative resolution.

---

## 3. Post-Calculation Condonation Boundary

Condonation MUST NEVER silently alter ordinary attendance calculations. Condonation is strictly a post-calculation layer that evaluates an already-computed shortage:

```
Raw Attendance
      ↓
Normal Attendance Result (SubjectAttendanceResult)
      ↓
Shortage Analysis (ShortageAnalysis)
      ↓
Condonation Policy (CondonationPolicy - blocked pending confirmation)
      ↓
Final Eligibility / Adjusted Result
```
Institutional rules **RIC-SC-003 through RIC-SC-006** remain pending confirmation before condonation implementation begins.

---

## 4. Overall Attendance Strategies & Integrated Course Participation

### 4.1 The Four Distinct Strategies
1. `ARITHMETIC_MEAN`: Unweighted percentage average.
2. `WEIGHTED_BY_CREDITS`: Weighted by credit hours.
3. `AGGREGATE_UNITS`: Discrete unit ratio.
4. `AGGREGATE_HOURS`: Clock/contact hour ratio ($A_s \times H_s / C_s \times H_s$).

### 4.2 Integrated Courses in Overall Attendance (RIC-OA-004)
Institutions can configure:
* **Component-Level Entry:** Theory and Lab components enter overall calculation as independent items.
* **Consolidated Entry:** The integrated course enters overall calculation as a single combined or weighted result.
