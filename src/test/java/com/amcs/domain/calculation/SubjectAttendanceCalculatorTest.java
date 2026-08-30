package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.test.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link SubjectAttendanceCalculator}.
 *
 * Test naming convention: given_[setup]_when_[action]_then_[expectation]
 */
@DisplayName("SubjectAttendanceCalculator")
class SubjectAttendanceCalculatorTest {

    private SubjectAttendanceCalculator calculator;
    private Enrollment fullEnrollment;
    private AttendancePolicy policy75;

    @BeforeEach
    void setUp() {
        calculator = new SubjectAttendanceCalculator();
        fullEnrollment = TestFixtures.fullEnrollment(STUDENT_1);
        policy75 = TestFixtures.policy75();
    }

    // =========================================================================
    // A) Zero and edge case handling
    // =========================================================================

    @Nested
    @DisplayName("A. Zero and edge cases")
    class ZeroAndEdgeCases {

        @Test
        @DisplayName("A-01: No sessions → UNDEFINED classification, zero percentage")
        void noSessions_returnsUndefined() {
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                List.of(), List.of(), fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
            assertThat(result.attendancePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.conductedUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("A-02: All sessions CANCELLED → UNDEFINED (cancelled never count)")
        void allSessionsCancelled_returnsUndefined() {
            var sessions = List.of(
                cancelledSession(UUID.randomUUID(), semDay(1)),
                cancelledSession(UUID.randomUUID(), semDay(2))
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, List.of(), fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }

        @Test
        @DisplayName("A-03: All sessions SCHEDULED (future) → UNDEFINED")
        void allSessionsScheduled_returnsUndefined() {
            var sessions = List.of(
                scheduledSession(UUID.randomUUID(), semDay(1))
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, List.of(), fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }

        @Test
        @DisplayName("A-04: 100% attendance → ADEQUATE with zero shortage")
        void perfectAttendance_adequateNoShortage() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2))
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.PRESENT),
                record(s2, STUDENT_1, AttendanceStatus.PRESENT)
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.shortageUnits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.surplusUnits()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("A-05: 0% attendance → SHORTAGE with full shortage")
        void zeroAttendance_shortage() {
            UUID s1 = UUID.randomUUID();
            var sessions = List.of(conductedTheorySession(s1, semDay(1)));
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.ABSENT));
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
            assertThat(result.attendancePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("A-06: Missing record: TREAT_AS_ABSENT strategy")
        void missingRecord_treatedAsAbsent() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2))
            );
            // Only record for s1 — s2 has no record
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

            var policy = AttendancePolicy.withDefaultContributions(
                POLICY_75, "Standard 75", new BigDecimal("75"),
                com.amcs.domain.policy.MissingRecordStrategy.TREAT_AS_ABSENT);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy,
                sessions, records, fullEnrollment, SEMESTER_1);

            // 1 present / 2 conducted = 50%
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
            assertThat(result.missingRecordCount()).isEqualTo(1);
            assertThat(result.missingRecordStrategy()).isEqualTo(com.amcs.domain.policy.MissingRecordStrategy.TREAT_AS_ABSENT);
            assertThat(result.isIncomplete()).isFalse();
        }

        @Test
        @DisplayName("A-07: Missing record: EXCLUDE_FROM_CALCULATION strategy")
        void missingRecord_excludedFromCalculation() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2))
            );
            // Record present for s1; s2 has no record
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

            var policy = AttendancePolicy.withDefaultContributions(
                POLICY_75, "Exclude Missing Policy", new BigDecimal("75"),
                com.amcs.domain.policy.MissingRecordStrategy.EXCLUDE_FROM_CALCULATION);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy,
                sessions, records, fullEnrollment, SEMESTER_1);

            // s2 excluded from both denominator and numerator: 1/1 = 100.00%
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
            assertThat(result.missingRecordCount()).isEqualTo(1);
            assertThat(result.missingRecordStrategy()).isEqualTo(com.amcs.domain.policy.MissingRecordStrategy.EXCLUDE_FROM_CALCULATION);
        }

        @Test
        @DisplayName("A-08: Missing record: MARK_AS_INCOMPLETE strategy")
        void missingRecord_markedAsIncomplete() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2))
            );
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

            var policy = AttendancePolicy.withDefaultContributions(
                POLICY_75, "Incomplete Policy", new BigDecimal("75"),
                com.amcs.domain.policy.MissingRecordStrategy.MARK_AS_INCOMPLETE);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.INCOMPLETE);
            assertThat(result.isIncomplete()).isTrue();
            assertThat(result.missingRecordCount()).isEqualTo(1);
            assertThat(result.missingRecordStrategy()).isEqualTo(com.amcs.domain.policy.MissingRecordStrategy.MARK_AS_INCOMPLETE);
        }

        @Test
        @DisplayName("A-09: Scheduled 3-unit lab that is CANCELLED contributes 0 conducted units")
        void cancelledThreeUnitLab_contributesZeroConductedUnits() {
            UUID s1 = UUID.randomUUID();
            // Timetable scheduled plannedUnits=3, but session is CANCELLED
            var cancelledLab = new Session(
                s1, SUBJECT_LAB, SECTION_1, FACULTY_1, semDay(5),
                com.amcs.domain.attendance.SessionType.LAB, 3, 0,
                SessionStatus.CANCELLED, Optional.empty(), Optional.empty());

            assertThat(cancelledLab.plannedUnits()).isEqualTo(3);
            assertThat(cancelledLab.conductedUnits()).isZero();
            assertThat(cancelledLab.isCountable()).isFalse();

            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                List.of(cancelledLab), List.of(), fullEnrollment, SEMESTER_1);

            assertThat(result.conductedUnits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }

        @Test
        @DisplayName("A-10: Non-conducted session with non-zero conductedUnits is rejected")
        void nonConductedWithNonZeroConductedUnits_throws() {
            UUID s1 = UUID.randomUUID();
            assertThatThrownBy(() -> new Session(
                s1, SUBJECT_LAB, SECTION_1, FACULTY_1, semDay(5),
                com.amcs.domain.attendance.SessionType.LAB, 3, 3, // Invalid: CANCELLED with conductedUnits=3
                SessionStatus.CANCELLED, Optional.empty(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conductedUnits must be 0 for non-CONDUCTED session");
        }
    }

    // =========================================================================
    // B) Standard theory attendance calculation
    // =========================================================================

    @Nested
    @DisplayName("B. Standard theory calculations")
    class TheoryCalculations {

        @Test
        @DisplayName("B-01: 75% threshold, exactly 75% attendance → ADEQUATE")
        void exactlyAtThreshold_adequate() {
            // 15/20 = 75.00% exactly
            var sessions = buildConductedSessions(theorySubject().id(), 20);
            var records = buildRecords(sessions, 15, 5);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
            assertThat(result.shortageUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("B-02: 74.99% (14/19) → SHORTAGE")
        void justBelowThreshold_shortage() {
            // 14/19 = 73.68%
            var sessions = buildConductedSessions(theorySubject().id(), 19);
            var records = buildRecords(sessions, 14, 5);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
            assertThat(result.attendancePercentage()).isLessThan(new BigDecimal("75.00"));
        }

        @Test
        @DisplayName("B-03: Mix of CONDUCTED and CANCELLED — cancelled excluded from denominator")
        void cancelledSessionsExcludedFromDenominator() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            UUID s3 = UUID.randomUUID(); // cancelled
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2)),
                cancelledSession(s3, semDay(3))
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.PRESENT),
                record(s2, STUDENT_1, AttendanceStatus.PRESENT)
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            // 2/2 = 100%, not 2/3
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("B-04: Double-period session (conductedUnits=2) counted correctly")
        void doublePeriodSession_correctUnitCount() {
            UUID s1 = UUID.randomUUID(); // 2-unit session
            UUID s2 = UUID.randomUUID(); // 1-unit session
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1), 2),
                conductedTheorySession(s2, semDay(2), 1)
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.PRESENT),
                record(s2, STUDENT_1, AttendanceStatus.ABSENT)
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            // conducted = 3, attended = 2 (2-unit session present, 1-unit absent)
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(result.attendancePercentage())
                .isEqualByComparingTo(new BigDecimal("66.67")); // 2/3 × 100 = 66.67
        }
    }

    // =========================================================================
    // C) Status contribution tests
    // =========================================================================

    @Nested
    @DisplayName("C. Status contributions")
    class StatusContributions {

        @Test
        @DisplayName("C-01: DUTY_LEAVE with contribution=1.0 counts as present")
        void dutyLeave_contributionOne_countsAsPresent() {
            UUID s1 = UUID.randomUUID();
            var sessions = List.of(conductedTheorySession(s1, semDay(1)));
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.DUTY_LEAVE));

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75, // policy75 gives DL = 1.0
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("C-02: DUTY_LEAVE with contribution=0.0 counts as absent")
        void dutyLeave_contributionZero_countsAsAbsent() {
            UUID s1 = UUID.randomUUID();
            var partialPolicy = TestFixtures.policyWithPartialContributions(
                UUID.randomUUID(), new BigDecimal("75"));
            var sessions = List.of(conductedTheorySession(s1, semDay(1)));
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.DUTY_LEAVE));

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), partialPolicy,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.attendedUnits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
        }

        @Test
        @DisplayName("C-03: MEDICAL_LEAVE with contribution=0.5 counts as half unit")
        void medicalLeave_halfContribution() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var partialPolicy = TestFixtures.policyWithPartialContributions(
                UUID.randomUUID(), new BigDecimal("75"));
            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),
                conductedTheorySession(s2, semDay(2))
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.PRESENT),     // 1.0
                record(s2, STUDENT_1, AttendanceStatus.MEDICAL_LEAVE) // 0.5
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), partialPolicy,
                sessions, records, fullEnrollment, SEMESTER_1);

            // attended = 1.0 + 0.5 = 1.5; conducted = 2; percentage = 75.00%
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("1.50"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
        }

        @Test
        @DisplayName("C-04: Unknown status not in policy → treated as ZERO contribution")
        void unknownStatus_notInPolicy_treatedAsAbsent() {
            UUID s1 = UUID.randomUUID();
            // Policy with ONLY PRESENT defined; ON_DUTY is missing from the map
            Map<AttendanceStatus, BigDecimal> contributions = new EnumMap<>(AttendanceStatus.class);
            contributions.put(AttendanceStatus.PRESENT, BigDecimal.ONE);
            var sparsePolicy = AttendancePolicy.withContributions(
                UUID.randomUUID(), "Sparse Policy", 1, new BigDecimal("75"), contributions);

            var sessions = List.of(conductedTheorySession(s1, semDay(1)));
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.ON_DUTY));

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), sparsePolicy,
                sessions, records, fullEnrollment, SEMESTER_1);

            // ON_DUTY not in map → getContribution returns ZERO → treated as absent
            assertThat(result.attendedUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // D) Enrollment window filtering
    // =========================================================================

    @Nested
    @DisplayName("D. Enrollment window")
    class EnrollmentWindow {

        @Test
        @DisplayName("D-01: Sessions before enrollment start are excluded")
        void sessionsBeforeEnrollmentStart_excluded() {
            // Sessions on day 1 and day 50; student enrolled from day 30
            UUID s1 = UUID.randomUUID(); // before enrollment
            UUID s2 = UUID.randomUUID(); // after enrollment
            LocalDate enrollStart = semDay(30);
            var enrollment = TestFixtures.midEnrollment(STUDENT_1, enrollStart);

            var sessions = List.of(
                conductedTheorySession(s1, semDay(1)),   // before
                conductedTheorySession(s2, semDay(50))   // after
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.ABSENT),
                record(s2, STUDENT_1, AttendanceStatus.PRESENT)
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, enrollment, SEMESTER_1);

            // Only s2 should count: 1/1 = 100%
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("D-02: Sessions after enrollment end are excluded")
        void sessionsAfterEnrollmentEnd_excluded() {
            UUID s1 = UUID.randomUUID(); // within
            UUID s2 = UUID.randomUUID(); // after end
            var enrollment = new Enrollment(STUDENT_1, SECTION_1,
                semDay(1), Optional.of(semDay(10)), Optional.empty());

            var sessions = List.of(
                conductedTheorySession(s1, semDay(5)),   // within window
                conductedTheorySession(s2, semDay(20))   // after end
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.PRESENT),
                record(s2, STUDENT_1, AttendanceStatus.PRESENT)
            );
            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, enrollment, SEMESTER_1);

            // Only s1: 1/1 = 100%
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
        }

        @Test
        @DisplayName("D-03: Sessions outside academic period are excluded even if within enrollment")
        void sessionsOutsideAcademicPeriod_excluded() {
            UUID s1 = UUID.randomUUID();
            // Session is within enrollment but OUTSIDE the academic period
            LocalDate outsidePeriod = SEMESTER_1.endDate().plusDays(10);
            var session = new com.amcs.domain.attendance.Session(
                s1, SUBJECT_THEORY, SECTION_1, FACULTY_1,
                outsidePeriod, com.amcs.domain.attendance.SessionType.THEORY, 1,
                SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                List.of(session), List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT)),
                fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }
    }

    // =========================================================================
    // E) Lab group filtering
    // =========================================================================

    @Nested
    @DisplayName("E. Lab group filtering")
    class LabGroupFiltering {

        @Test
        @DisplayName("E-01: Student in Group A excluded from Group B sessions")
        void studentGroupA_excludedFromGroupBSession() {
            UUID s1 = UUID.randomUUID(); // Group B lab session
            var session = conductedLabSessionForGroup(s1, semDay(1), 3, LAB_GROUP_B);
            var enrollment = enrollmentWithLabGroup(STUDENT_1, LAB_GROUP_A);

            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                List.of(session), List.of(), enrollment, SEMESTER_1);

            // Group B session must not count for Group A student
            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }

        @Test
        @DisplayName("E-02: Non-group-specific session counts for all students")
        void noGroupSession_countsForAllStudents() {
            UUID s1 = UUID.randomUUID();
            var session = conductedLabSession(s1, semDay(1), 1);
            var enrollment = enrollmentWithLabGroup(STUDENT_1, LAB_GROUP_A);

            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                List.of(session), List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT)),
                enrollment, SEMESTER_1);

            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
        }

        @Test
        @DisplayName("E-03: Student with no lab group excluded from group-specific sessions")
        void studentNoGroup_excludedFromGroupSession() {
            UUID s1 = UUID.randomUUID();
            var session = conductedLabSessionForGroup(s1, semDay(1), 3, LAB_GROUP_A);
            var enrollment = TestFixtures.fullEnrollment(STUDENT_1); // no lab group

            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                List.of(session), List.of(), enrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.UNDEFINED);
        }
    }

    // =========================================================================
    // F) Shortage and surplus units
    // =========================================================================

    @Nested
    @DisplayName("F. Shortage and surplus units")
    class ShortageAndSurplus {

        @Test
        @DisplayName("F-01: Shortage units correctly computed (10/20 = 50%, threshold 75%)")
        void shortageUnits_correct() {
            // 10/20 = 50%; need 15/20 = 75%; shortage = 5 units
            var sessions = buildConductedSessions(theorySubject().id(), 20);
            var records = buildRecords(sessions, 10, 10);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
            assertThat(result.shortageUnits()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.surplusUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("F-02: Surplus units correctly computed (18/20 = 90%, threshold 75%)")
        void surplusUnits_correct() {
            // 18/20 = 90%; required = 15/20; surplus = 3 units
            var sessions = buildConductedSessions(theorySubject().id(), 20);
            var records = buildRecords(sessions, 18, 2);

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
            assertThat(result.surplusUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.shortageUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // G) Policy metadata capture
    // =========================================================================

    @Nested
    @DisplayName("G. Policy metadata capture")
    class PolicyMetadata {

        @Test
        @DisplayName("G-01: Result captures correct policyId and policyVersion")
        void result_capturesPolicyMetadata() {
            UUID s1 = UUID.randomUUID();
            var sessions = List.of(conductedTheorySession(s1, semDay(1)));
            var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

            var result = calculator.calculate(
                STUDENT_1, theorySubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            assertThat(result.policyId()).isEqualTo(POLICY_75);
            assertThat(result.policyVersion()).isEqualTo(1);
        }
    }

    // =========================================================================
    // H) Laboratory session multi-unit counting
    // =========================================================================

    @Nested
    @DisplayName("H. Lab session unit counting")
    class LabUnitCounting {

        @Test
        @DisplayName("H-01: 3-hour lab session (conductedUnits=3) — present counts 3 units")
        void threeHourLab_present_threeUnits() {
            UUID s1 = UUID.randomUUID();
            var session = conductedLabSession(s1, semDay(1), 3);
            var record = record(s1, STUDENT_1, AttendanceStatus.PRESENT);

            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                List.of(session), List.of(record), fullEnrollment, SEMESTER_1);

            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("H-02: 3-hour lab session (conductedUnits=3) — absent counts 0 units")
        void threeHourLab_absent_zeroUnits() {
            UUID s1 = UUID.randomUUID();
            UUID s2 = UUID.randomUUID();
            var sessions = List.of(
                conductedLabSession(s1, semDay(1), 3), // absent
                conductedLabSession(s2, semDay(8), 3)  // present
            );
            var records = List.of(
                record(s1, STUDENT_1, AttendanceStatus.ABSENT),
                record(s2, STUDENT_1, AttendanceStatus.PRESENT)
            );
            var result = calculator.calculate(
                STUDENT_1, labSubject(), policy75,
                sessions, records, fullEnrollment, SEMESTER_1);

            // conducted = 6, attended = 3 → 50%
            assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("6.00"));
            assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
        }
    }

    // =========================================================================
    // Test helpers
    // =========================================================================

    /**
     * Builds N conducted theory sessions within the semester.
     */
    private List<Session> buildConductedSessions(UUID subjectId, int count) {
        var sessions = new java.util.ArrayList<Session>(count);
        for (int i = 0; i < count; i++) {
            sessions.add(conductedTheorySession(UUID.randomUUID(), semDay(i + 1)));
        }
        return sessions;
    }

    /**
     * Builds a list of attendance records: {@code present} PRESENT records followed by
     * {@code absent} ABSENT records, mapped to the first N sessions.
     */
    private List<AttendanceRecord> buildRecords(List<Session> sessions, int present, int absent) {
        var records = new java.util.ArrayList<AttendanceRecord>(present + absent);
        for (int i = 0; i < present + absent && i < sessions.size(); i++) {
            AttendanceStatus status = (i < present)
                ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            records.add(record(sessions.get(i).id(), STUDENT_1, status));
        }
        return records;
    }
}
