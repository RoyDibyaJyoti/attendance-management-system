# Attendance Management and Calculation System (AMCS)
# ATTENDANCE_ENGINE.md — Calculation Engine Specifications

**Document Status:** Complete — Phase 1.5 Hardening Baseline  
**Version:** 1.1.0  
**Date:** 2026-08-29  
**Module:** `attendance-domain` (`com.amcs.domain.calculation.*`)

---

## 1. Engine Overview & Purity Guarantees

The AMCS Calculation Engine is a purely functional, deterministic computational core. It performs zero I/O, has zero persistence dependencies, and guarantees exact decimal arithmetic via `BigDecimal`.

---

## 2. Subject-Level Calculation Algorithm

```mermaid
flowchart TD
    Start([Start Calculation]) --> Init[Init totalConducted = 0, totalAttended = 0, missingRecords = 0]
    Init --> LoopSessions{For each session}
    LoopSessions -- Next Session --> G1{Gate 1: isCountable?}
    G1 -- No (Cancelled/Sched/Resched) --> LoopSessions
    G1 -- Yes --> G2{Gate 2: Period.contains?}
    G2 -- No --> LoopSessions
    G2 -- Yes --> G3{Gate 3: Enrollment.isActiveOn?}
    G3 -- No --> LoopSessions
    G3 -- Yes --> G4{Gate 4: Enrollment.isEligibleForLabGroup?}
    G4 -- No --> LoopSessions
    G4 -- Yes --> CheckRecord{Record exists for student?}
    
    CheckRecord -- No --> MissingAction{policy.missingRecordStrategy}
    MissingAction -- EXCLUDE --> LoopSessions
    MissingAction -- TREAT_AS_ABSENT --> AccAbsent[totalConducted += session.conductedUnits<br/>totalAttended += 0<br/>missingRecords++]
    MissingAction -- MARK_INCOMPLETE --> AccIncomplete[totalConducted += session.conductedUnits<br/>totalAttended += 0<br/>missingRecords++]
    AccAbsent --> LoopSessions
    AccIncomplete --> LoopSessions

    CheckRecord -- Yes --> AccNormal[totalConducted += session.conductedUnits<br/>totalAttended += session.conductedUnits * policy.getContribution(status)]
    AccNormal --> LoopSessions

    LoopSessions -- Done --> CheckMissing{missingRecords > 0 &&<br/>strategy == MARK_INCOMPLETE?}
    CheckMissing -- Yes --> RetIncomplete[classification = INCOMPLETE]
    CheckMissing -- No --> CheckZero{totalConducted == 0?}
    CheckZero -- Yes --> RetUndefined[classification = UNDEFINED]
    CheckZero -- No --> Classify{percentage >= threshold?}
    Classify -- Yes --> RetAdequate[classification = ADEQUATE]
    Classify -- No --> RetShortage[classification = SHORTAGE]
```

---

## 3. The Four Distinct Overall Aggregation Strategies

| Strategy | Formula | Mathematical Behavior |
|---|---|---|
| **`ARITHMETIC_MEAN`** | $\frac{1}{N} \sum_{i=1}^N P_i$ | Each course has identical weight, regardless of credits or conducted units. |
| **`WEIGHTED_BY_CREDITS`** | $\frac{\sum (\text{Credits}_i \times P_i)}{\sum \text{Credits}_i}$ | Courses with higher credit hours exert greater influence. |
| **`AGGREGATE_UNITS`** | $\frac{\sum A_i}{\sum C_i} \times 100$ | Raw ratio across all discrete units. |
| **`AGGREGATE_HOURS`** | $\frac{\sum (A_i \times H_i)}{\sum (C_i \times H_i)} \times 100$ | Each subject's units are scaled by contact hours per unit ($H_i$). Diverges from units when courses have unequal hourly durations. |

### Comparative Proof (Identical Student Dataset)
* **Subject 1 (Theory, 2 Credits, 1 hr/unit):** 10 attended / 20 conducted units (50.00%)
* **Subject 2 (Lab, 4 Credits, 3 hrs/unit):** 9 attended / 10 conducted units (90.00%)

Results:
* **`ARITHMETIC_MEAN`:** $(50.00 + 90.00) / 2 =$ **70.00%**
* **`AGGREGATE_UNITS`:** $(10 + 9) / (20 + 10) = 19 / 30 =$ **63.33%**
* **`AGGREGATE_HOURS`:** $(10 \times 1 + 9 \times 3) / (20 \times 1 + 10 \times 3) = 37 / 50 =$ **74.00%**
* **`WEIGHTED_BY_CREDITS`:** $(50 \times 2 + 90 \times 4) / 6 = 460 / 6 =$ **76.67%**

All four results are strictly non-coincidental and mathematically distinct.

---

## 4. Mathematical Shortage & Prediction Solvers

### Forward Formula (Minimum Units to Attend)
$$x_{\min} = \max\left(0, \left\lceil \frac{T \cdot C - A}{1 - T} \right\rceil\right)$$
* Verified safe up to 100,000 units with exact integer boundaries.
* If $T = 100\%$ and $A < C$: Returns $-1$ (mathematically unreachable).

### Backward Formula (Maximum Future Absences)
$$y_{\max} = \max\left(0, \min\left(R, \left\lfloor A + R(1 - T) - T \cdot C \right\rfloor\right)\right)$$
* Clamped within $[0, R]$.
