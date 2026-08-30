package com.amcs.domain.calculation;

import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.result.IntegratedSubjectAttendanceResult;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.IntegratedCompositeStrategy;
import com.amcs.domain.policy.IntegratedSubjectPolicy;
import com.amcs.domain.test.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("IntegratedSubjectCalculator")
class IntegratedSubjectCalculatorTest {

    private IntegratedSubjectCalculator calculator;
    private AttendancePolicy theoryPolicy;
    private AttendancePolicy labPolicy;

    @BeforeEach
    void setUp() {
        calculator = new IntegratedSubjectCalculator(new SubjectAttendanceCalculator());
        theoryPolicy = TestFixtures.policy75();
        labPolicy = TestFixtures.policy80();
    }

    @Nested
    @DisplayName("IS-A. SEPARATE_THRESHOLDS strategy")
    class SeparateThresholds {

        @Test
        @DisplayName("IS-A-01: Both adequate → not in shortage")
        void bothAdequate_notShortage() {
            // Theory: 16/20 = 80% ≥ 75% ✓
            // Lab: 9/10 = 90% ≥ 80% ✓
            UUID t1 = UUID.randomUUID(), t2 = UUID.randomUUID();
            UUID l1 = UUID.randomUUID();

            var sessions = buildIntegratedSessions(16, 4, 9, 1);
            var records = buildIntegratedRecords(sessions, 16, 9);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.separateThresholds(),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.strategy()).isEqualTo(IntegratedCompositeStrategy.SEPARATE_THRESHOLDS);
            assertThat(result.isShortage()).isFalse();
            assertThat(result.theoryComponent().result().isAdequate()).isTrue();
            assertThat(result.labComponent().result().isAdequate()).isTrue();
        }

        @Test
        @DisplayName("IS-A-02: Lab in shortage, theory adequate → overall shortage")
        void labShortage_theoryAdequate_isShortage() {
            // Theory: 16/20 = 80% ≥ 75% ✓
            // Lab: 7/10 = 70% < 80% ✗
            var sessions = buildIntegratedSessions(16, 4, 7, 3);
            var records = buildIntegratedRecords(sessions, 16, 7);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.separateThresholds(),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.isShortage()).isTrue();
            assertThat(result.labComponent().result().isShortage()).isTrue();
            assertThat(result.theoryComponent().result().isAdequate()).isTrue();
        }

        @Test
        @DisplayName("IS-A-03: Theory in shortage, lab adequate → overall shortage")
        void theoryShortage_labAdequate_isShortage() {
            // Theory: 10/20 = 50% < 75% ✗
            // Lab: 9/10 = 90% ≥ 80% ✓
            var sessions = buildIntegratedSessions(10, 10, 9, 1);
            var records = buildIntegratedRecords(sessions, 10, 9);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.separateThresholds(),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.isShortage()).isTrue();
            assertThat(result.theoryComponent().result().isShortage()).isTrue();
        }

        @Test
        @DisplayName("IS-A-04: No combined fields populated for SEPARATE_THRESHOLDS")
        void separateThresholds_noCombinedFields() {
            var sessions = buildIntegratedSessions(16, 4, 9, 1);
            var records = buildIntegratedRecords(sessions, 16, 9);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.separateThresholds(),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.combinedResult()).isEmpty();
            assertThat(result.compositePercentage()).isEmpty();
            assertThat(result.compositeThreshold()).isEmpty();
        }
    }

    @Nested
    @DisplayName("IS-B. COMBINED_ALL_SESSIONS strategy")
    class CombinedAllSessions {

        @Test
        @DisplayName("IS-B-01: Combined 75% policy — all sessions pooled")
        void combinedPolicy_poolsAllSessions() {
            // 16 theory (present) + 4 theory (absent) + 9 lab (present) + 1 lab (absent)
            // Total: 25 present / 30 conducted = 83.33%  ≥ 75% ✓
            var sessions = buildIntegratedSessions(16, 4, 9, 1);
            var records = buildIntegratedRecords(sessions, 16, 9);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.combinedAllSessions(theoryPolicy),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.strategy()).isEqualTo(IntegratedCompositeStrategy.COMBINED_ALL_SESSIONS);
            assertThat(result.combinedResult()).isPresent();
            assertThat(result.combinedResult().get().conductedUnits())
                .isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.combinedResult().get().attendedUnits())
                .isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.isShortage()).isFalse();
        }
    }

    @Nested
    @DisplayName("IS-C. WEIGHTED_AVERAGE strategy")
    class WeightedAverage {

        @Test
        @DisplayName("IS-C-01: Weighted average (0.6 theory + 0.4 lab) correctly computed")
        void weightedAverage_correctComputation() {
            // Theory: 16/20 = 80.00% × 0.6 = 48.00
            // Lab: 7/10 = 70.00% × 0.4 = 28.00
            // Composite = 76.00% ≥ 75% ✓
            var sessions = buildIntegratedSessions(16, 4, 7, 3);
            var records = buildIntegratedRecords(sessions, 16, 7);

            var policy = IntegratedSubjectPolicy.weightedAverage(
                new BigDecimal("0.6"), new BigDecimal("0.4"), new BigDecimal("75"));

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(TestFixtures.policy75()), // 75% lab threshold
                policy,
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.strategy()).isEqualTo(IntegratedCompositeStrategy.WEIGHTED_AVERAGE);
            assertThat(result.compositePercentage()).isPresent();
            assertThat(result.compositePercentage().get()).isEqualByComparingTo(new BigDecimal("76.00"));
            assertThat(result.isShortage()).isFalse();
        }

        @Test
        @DisplayName("IS-C-02: Weighted average below threshold → shortage")
        void weightedAverage_belowThreshold_shortage() {
            // Theory: 10/20 = 50% × 0.6 = 30
            // Lab: 7/10 = 70% × 0.4 = 28
            // Composite = 58% < 75% → shortage
            var sessions = buildIntegratedSessions(10, 10, 7, 3);
            var records = buildIntegratedRecords(sessions, 10, 7);

            var policy = IntegratedSubjectPolicy.weightedAverage(
                new BigDecimal("0.6"), new BigDecimal("0.4"), new BigDecimal("75"));

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(TestFixtures.policy75()),
                policy, sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.compositePercentage().get()).isEqualByComparingTo(new BigDecimal("58.00"));
            assertThat(result.isShortage()).isTrue();
        }

        @Test
        @DisplayName("IS-C-03: Weights that don't sum to 1.0 → rejected at policy creation")
        void invalidWeights_rejectedAtPolicyCreation() {
            assertThatThrownBy(() ->
                IntegratedSubjectPolicy.weightedAverage(
                    new BigDecimal("0.6"), new BigDecimal("0.5"), new BigDecimal("75")))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // IS-D. Integrated subject — no sessions for one component
    // =========================================================================

    @Nested
    @DisplayName("IS-D. One component has no sessions")
    class OneComponentNoSessions {

        @Test
        @DisplayName("IS-D-01: No lab sessions — lab component is UNDEFINED, theory adequate")
        void noLabSessions_labUndefined() {
            // Only theory sessions exist
            var sessions = buildTheoryOnly(16, 4);
            var records = buildIntegratedRecords(sessions, 16, 0);

            var result = calculator.calculate(
                STUDENT_1, integratedSubject(),
                TestFixtures.theoryComponent(theoryPolicy),
                TestFixtures.labComponent(labPolicy),
                IntegratedSubjectPolicy.separateThresholds(),
                sessions, records,
                TestFixtures.fullEnrollment(STUDENT_1), SEMESTER_1);

            assertThat(result.labComponent().result().isUndefined()).isTrue();
            assertThat(result.theoryComponent().result().isAdequate()).isTrue();
            // UNDEFINED lab does NOT cause shortage in SEPARATE_THRESHOLDS
            assertThat(result.isShortage()).isFalse();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a mixed list of theory (present+absent) and lab (present+absent) sessions.
     */
    private List<Session> buildIntegratedSessions(
        int theoryPresent, int theoryAbsent, int labPresent, int labAbsent) {
        var sessions = new java.util.ArrayList<Session>();
        int day = 1;
        for (int i = 0; i < theoryPresent + theoryAbsent; i++) {
            sessions.add(integratedTheorySession(UUID.randomUUID(), semDay(day++)));
        }
        for (int i = 0; i < labPresent + labAbsent; i++) {
            sessions.add(integratedLabSession(UUID.randomUUID(), semDay(day++), 1));
        }
        return sessions;
    }

    private List<Session> buildTheoryOnly(int present, int absent) {
        var sessions = new java.util.ArrayList<Session>();
        for (int i = 0; i < present + absent; i++) {
            sessions.add(integratedTheorySession(UUID.randomUUID(), semDay(i + 1)));
        }
        return sessions;
    }

    /**
     * Builds records: first theoryPresent theory sessions are PRESENT, rest ABSENT;
     * first labPresent lab sessions are PRESENT, rest ABSENT.
     */
    private List<AttendanceRecord> buildIntegratedRecords(
        List<Session> sessions, int theoryPresent, int labPresent) {
        var records = new java.util.ArrayList<AttendanceRecord>();
        int theoryCount = 0, labCount = 0;
        for (Session s : sessions) {
            AttendanceStatus status;
            if (s.sessionType() == com.amcs.domain.attendance.SessionType.THEORY) {
                status = theoryCount++ < theoryPresent
                    ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            } else {
                status = labCount++ < labPresent
                    ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            }
            records.add(record(s.id(), STUDENT_1, status));
        }
        return records;
    }
}
