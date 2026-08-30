package com.amcs.domain.calculation;

import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.calculation.result.PredictionResult;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.test.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("PredictionEngine")
class PredictionEngineTest {

    private PredictionEngine engine;
    private SubjectAttendanceCalculator subjectCalc;
    private AttendancePolicy policy75;

    @BeforeEach
    void setUp() {
        engine = new PredictionEngine(new ShortageCalculator());
        subjectCalc = new SubjectAttendanceCalculator();
        policy75 = TestFixtures.policy75();
    }

    // =========================================================================
    // Helpers to build SubjectAttendanceResult directly
    // =========================================================================

    private SubjectAttendanceResult buildResult(int attended, int conducted, BigDecimal threshold) {
        BigDecimal a = BigDecimal.valueOf(attended);
        BigDecimal c = BigDecimal.valueOf(conducted);
        BigDecimal pct = c.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : a.multiply(new BigDecimal("100")).divide(c, 2, java.math.RoundingMode.HALF_UP);

        AttendanceClassification cls;
        if (c.compareTo(BigDecimal.ZERO) == 0) cls = AttendanceClassification.UNDEFINED;
        else cls = pct.compareTo(threshold) >= 0
            ? AttendanceClassification.ADEQUATE : AttendanceClassification.SHORTAGE;

        return new SubjectAttendanceResult(
            STUDENT_1, SUBJECT_THEORY, POLICY_75, 1, SEMESTER_1,
            c, a, pct, threshold, cls,
            BigDecimal.ZERO, BigDecimal.ZERO, Instant.now()
        );
    }

    // =========================================================================
    // P-01 to P-0N: Prediction tests
    // =========================================================================

    @Nested
    @DisplayName("P. Prediction engine cases")
    class PredictionCases {

        @Test
        @DisplayName("P-01: UNDEFINED (no sessions) — prediction from zero")
        void undefined_predictionFromZero() {
            var result = buildResult(0, 0, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 20);

            assertThat(prediction.canReachTarget()).isTrue();
            // Need to attend ceil(0.75 × 20) = ceil(15) = 15 sessions
            assertThat(prediction.minimumUnitsToAttend()).isEqualTo(15);
            // Can miss 20 - 15 = 5
            assertThat(prediction.maximumUnitsCanMiss()).isEqualTo(5);
            assertThat(prediction.isAlreadyAboveTarget()).isFalse();
        }

        @Test
        @DisplayName("P-02: Already above target → 0 minimum, can miss some")
        void alreadyAbove_zeroMinimum() {
            var result = buildResult(18, 20, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 20);

            assertThat(prediction.isAlreadyAboveTarget()).isTrue();
            assertThat(prediction.minimumUnitsToAttend()).isZero();
            assertThat(prediction.canReachTarget()).isTrue();
            assertThat(prediction.maximumUnitsCanMiss()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("P-03: In shortage, enough remaining to recover")
        void inShortage_canRecover() {
            // 10/20 = 50%; need 20 more to reach 75%: (10+20)/(20+20)=30/40=75%
            var result = buildResult(10, 20, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 30);

            assertThat(prediction.canReachTarget()).isTrue();
            assertThat(prediction.minimumUnitsToAttend()).isEqualTo(20);
            assertThat(prediction.remainingUnits()).isEqualTo(30);
        }

        @Test
        @DisplayName("P-04: In shortage, NOT enough remaining to recover")
        void inShortage_cannotRecover() {
            // 10/20 = 50%; need 20 more, but only 10 remaining
            var result = buildResult(10, 20, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 10);

            assertThat(prediction.canReachTarget()).isFalse();
            assertThat(prediction.maximumUnitsCanMiss()).isZero();
        }

        @Test
        @DisplayName("P-05: T=100%, one past absence → canReachTarget=false")
        void hundredTarget_oneAbsence_cannotReach() {
            var result = buildResult(19, 20, new BigDecimal("100"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 100);

            assertThat(prediction.canReachTarget()).isFalse();
        }

        @Test
        @DisplayName("P-06: Zero remaining sessions")
        void zeroRemaining_noProjection() {
            var result = buildResult(10, 20, new BigDecimal("75")); // 50% — in shortage
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 0);

            assertThat(prediction.remainingUnits()).isZero();
            assertThat(prediction.canReachTarget()).isFalse();
            assertThat(prediction.maximumUnitsCanMiss()).isZero();
        }

        @Test
        @DisplayName("P-07: Exactly at threshold with remaining — can miss some")
        void exactlyAtThreshold_withRemaining_canMiss() {
            // 15/20 = 75% exactly; 20 remaining
            var result = buildResult(15, 20, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 20);

            assertThat(prediction.isAlreadyAboveTarget()).isTrue();
            // y_max = ⌊15 + 20×0.25 − 0.75×20⌋ = ⌊15 + 5 − 15⌋ = 5
            assertThat(prediction.maximumUnitsCanMiss()).isEqualTo(5);
        }

        @Test
        @DisplayName("P-08: minimumUnitsToAttend is always non-negative")
        void minimumUnitsToAttend_neverNegative() {
            // Even if already above threshold
            var result = buildResult(20, 20, new BigDecimal("75"));
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, 10);

            assertThat(prediction.minimumUnitsToAttend()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("P-09: maximumUnitsCanMiss does not exceed remaining")
        void maximumUnitsMissable_doesNotExceedRemaining() {
            int remaining = 5;
            var result = buildResult(20, 20, new BigDecimal("50")); // 100% — way above
            var prediction = engine.predict(STUDENT_1, SUBJECT_THEORY, result, remaining);

            assertThat(prediction.maximumUnitsCanMiss()).isLessThanOrEqualTo(remaining);
        }

        @Test
        @DisplayName("P-10: null inputs → NullPointerException with clear message")
        void nullInputs_throw() {
            var result = buildResult(10, 20, new BigDecimal("75"));
            assertThatNullPointerException()
                .isThrownBy(() -> engine.predict(null, SUBJECT_THEORY, result, 10))
                .withMessageContaining("studentId");
            assertThatNullPointerException()
                .isThrownBy(() -> engine.predict(STUDENT_1, null, result, 10))
                .withMessageContaining("subjectId");
        }
    }
}
