package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.test.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Property-based invariant tests for the attendance calculation domain.
 *
 * These tests verify mathematical invariants that MUST hold for ALL valid inputs,
 * regardless of specific values. A failure here indicates a fundamental calculation bug.
 */
@DisplayName("Attendance Domain Invariants")
class AttendanceInvariantsTest {

    private SubjectAttendanceCalculator calculator;
    private AttendancePolicy policy75;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        calculator = new SubjectAttendanceCalculator();
        policy75 = TestFixtures.policy75();
        enrollment = TestFixtures.fullEnrollment(STUDENT_1);
    }

    @Test
    @DisplayName("INV-01: attendedUnits <= conductedUnits always")
    void attendedNeverExceedsConducted() {
        // Build various combinations
        for (int n = 1; n <= 20; n++) {
            for (int p = 0; p <= n; p++) {
                var sessions = buildSessions(n);
                var records = buildRecords(sessions, p);
                var result = calculator.calculate(
                    STUDENT_1, theorySubject(), policy75,
                    sessions, records, enrollment, SEMESTER_1);

                assertThat(result.attendedUnits())
                    .as("Invariant: attended <= conducted for n=%d, p=%d", n, p)
                    .isLessThanOrEqualTo(result.conductedUnits());
            }
        }
    }

    @Test
    @DisplayName("INV-02: percentage = (attended/conducted) × 100 [when denominator > 0]")
    void percentageMatchesFormula() {
        for (int n = 1; n <= 10; n++) {
            for (int p = 0; p <= n; p++) {
                var sessions = buildSessions(n);
                var records = buildRecords(sessions, p);
                var result = calculator.calculate(
                    STUDENT_1, theorySubject(), policy75,
                    sessions, records, enrollment, SEMESTER_1);

                if (!result.isUndefined()) {
                    BigDecimal expected = result.attendedUnits()
                        .multiply(new BigDecimal("100"))
                        .divide(result.conductedUnits(), 2, RoundingMode.HALF_UP);
                    assertThat(result.attendancePercentage())
                        .as("Percentage formula for n=%d, p=%d", n, p)
                        .isEqualByComparingTo(expected);
                }
            }
        }
    }

    @Test
    @DisplayName("INV-03: If ADEQUATE, shortageUnits = 0; if SHORTAGE, surplusUnits = 0")
    void shortageAndSurplus_mutuallyExclusive() {
        for (int n = 1; n <= 20; n++) {
            for (int p = 0; p <= n; p++) {
                var sessions = buildSessions(n);
                var records = buildRecords(sessions, p);
                var result = calculator.calculate(
                    STUDENT_1, theorySubject(), policy75,
                    sessions, records, enrollment, SEMESTER_1);

                if (result.isAdequate()) {
                    assertThat(result.shortageUnits())
                        .as("Adequate result must have 0 shortage, n=%d, p=%d", n, p)
                        .isEqualByComparingTo(BigDecimal.ZERO);
                } else if (result.isShortage()) {
                    assertThat(result.surplusUnits())
                        .as("Shortage result must have 0 surplus, n=%d, p=%d", n, p)
                        .isEqualByComparingTo(BigDecimal.ZERO);
                }
            }
        }
    }

    @Test
    @DisplayName("INV-04: shortageUnits >= 0 and surplusUnits >= 0 always")
    void shortageAndSurplus_neverNegative() {
        for (int n = 1; n <= 20; n++) {
            for (int p = 0; p <= n; p++) {
                var sessions = buildSessions(n);
                var records = buildRecords(sessions, p);
                var result = calculator.calculate(
                    STUDENT_1, theorySubject(), policy75,
                    sessions, records, enrollment, SEMESTER_1);

                assertThat(result.shortageUnits()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(result.surplusUnits()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            }
        }
    }

    @Test
    @DisplayName("INV-05: CANCELLED sessions never affect the denominator")
    void cancelledSessions_neverAffectDenominator() {
        // Build 10 conducted sessions
        var conducted = buildSessions(10);
        var records = buildRecords(conducted, 7);

        var result1 = calculator.calculate(
            STUDENT_1, theorySubject(), policy75,
            conducted, records, enrollment, SEMESTER_1);

        // Add 5 cancelled sessions to the list
        var withCancelled = new ArrayList<>(conducted);
        for (int i = 0; i < 5; i++) {
            withCancelled.add(new Session(UUID.randomUUID(), SUBJECT_THEORY, SECTION_1,
                FACULTY_1, semDay(100 + i), SessionType.THEORY, 1,
                SessionStatus.CANCELLED, Optional.empty(), Optional.empty()));
        }

        var result2 = calculator.calculate(
            STUDENT_1, theorySubject(), policy75,
            withCancelled, records, enrollment, SEMESTER_1);

        // The denominator and numerator must be identical
        assertThat(result1.conductedUnits())
            .isEqualByComparingTo(result2.conductedUnits());
        assertThat(result1.attendedUnits())
            .isEqualByComparingTo(result2.attendedUnits());
        assertThat(result1.attendancePercentage())
            .isEqualByComparingTo(result2.attendancePercentage());
    }

    @Test
    @DisplayName("INV-06: Adding attendance record for other student doesn't change result")
    void otherStudentRecords_noEffect() {
        var sessions = buildSessions(5);
        var ownRecords = buildRecords(sessions, 3);

        var result1 = calculator.calculate(
            STUDENT_1, theorySubject(), policy75,
            sessions, ownRecords, enrollment, SEMESTER_1);

        // Add records for a different student
        var allRecords = new ArrayList<>(ownRecords);
        for (var session : sessions) {
            allRecords.add(record(session.id(), STUDENT_2, AttendanceStatus.PRESENT));
        }

        var result2 = calculator.calculate(
            STUDENT_1, theorySubject(), policy75,
            sessions, allRecords, enrollment, SEMESTER_1);

        assertThat(result1.attendedUnits()).isEqualByComparingTo(result2.attendedUnits());
        assertThat(result1.attendancePercentage()).isEqualByComparingTo(result2.attendancePercentage());
    }

    @Test
    @DisplayName("INV-07: percentageInRange [0, 100] for all valid inputs")
    void percentageAlwaysInValidRange() {
        for (int n = 0; n <= 20; n++) {
            for (int p = 0; p <= n; p++) {
                var sessions = buildSessions(n);
                var records = buildRecords(sessions, p);
                var result = calculator.calculate(
                    STUDENT_1, theorySubject(), policy75,
                    sessions, records, enrollment, SEMESTER_1);

                assertThat(result.attendancePercentage())
                    .as("Percentage in [0,100] for n=%d, p=%d", n, p)
                    .isBetween(BigDecimal.ZERO, new BigDecimal("100"));
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<Session> buildSessions(int count) {
        var list = new ArrayList<Session>(count);
        for (int i = 0; i < count; i++) {
            list.add(conductedTheorySession(UUID.randomUUID(), semDay(i + 1)));
        }
        return list;
    }

    private List<AttendanceRecord> buildRecords(List<Session> sessions, int presentCount) {
        var list = new ArrayList<AttendanceRecord>(sessions.size());
        for (int i = 0; i < sessions.size(); i++) {
            AttendanceStatus status = i < presentCount
                ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            list.add(record(sessions.get(i).id(), STUDENT_1, status));
        }
        return list;
    }
}
