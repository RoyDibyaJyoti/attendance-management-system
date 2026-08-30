package com.amcs.domain.calculation;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.calculation.result.PredictionResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Mathematical Audit & Extreme Boundary Tests")
class MathematicalAuditTest {

    private ShortageCalculator shortageCalc;
    private SubjectAttendanceCalculator subjectCalc;
    private PredictionEngine predictionEngine;

    @BeforeEach
    void setUp() {
        shortageCalc = new ShortageCalculator();
        subjectCalc = new SubjectAttendanceCalculator();
        predictionEngine = new PredictionEngine(shortageCalc);
    }

    @Test
    @DisplayName("MA-01: Very large unit counts (100,000 units) calculate without overflow or precision loss")
    void veryLargeUnitCounts_calculateAccurately() {
        BigDecimal attended = new BigDecimal("75000");
        BigDecimal conducted = new BigDecimal("100000");
        BigDecimal target = new BigDecimal("75");

        // Exactly at 75%
        int minRequired = shortageCalc.computeMinimumUnitsRequired(attended, conducted, target);
        assertThat(minRequired).isZero();

        // 74,999 / 100,000 = 74.999% < 75%
        BigDecimal attendedShort = new BigDecimal("74999");
        int minShort = shortageCalc.computeMinimumUnitsRequired(attendedShort, conducted, target);
        // (74999 + x) / (100000 + x) >= 0.75 -> x >= (75000 - 74999) / 0.25 = 1 / 0.25 = 4
        assertThat(minShort).isEqualTo(4);

        // Verify: (74999 + 4) / (100000 + 4) = 75003 / 100004 = 75.0002% >= 75%
        double ratio = (74999.0 + 4) / (100000.0 + 4) * 100;
        assertThat(ratio).isGreaterThanOrEqualTo(75.0);
    }

    @Test
    @DisplayName("MA-02: Boundary: Target 0% always requires 0 additional units and allows missing all remaining")
    void targetZero_boundary() {
        BigDecimal attended = BigDecimal.ZERO;
        BigDecimal conducted = new BigDecimal("50");
        BigDecimal target = BigDecimal.ZERO;

        int minReq = shortageCalc.computeMinimumUnitsRequired(attended, conducted, target);
        assertThat(minReq).isZero();

        int maxMiss = shortageCalc.computeMaximumUnitsMissable(attended, conducted, 25, target);
        assertThat(maxMiss).isEqualTo(25);
    }

    @Test
    @DisplayName("MA-03: Boundary: Target 100% with perfect attendance requires 0 additional")
    void targetHundred_perfectAttendance() {
        BigDecimal attended = new BigDecimal("50");
        BigDecimal conducted = new BigDecimal("50");
        BigDecimal target = new BigDecimal("100");

        int minReq = shortageCalc.computeMinimumUnitsRequired(attended, conducted, target);
        assertThat(minReq).isZero();

        int maxMiss = shortageCalc.computeMaximumUnitsMissable(attended, conducted, 20, target);
        assertThat(maxMiss).isZero(); // Cannot miss any class if target is 100%
    }

    @Test
    @DisplayName("MA-04: Boundary: Target 100% with a single absence is mathematically impossible to reach")
    void targetHundred_singleAbsence_impossible() {
        BigDecimal attended = new BigDecimal("49");
        BigDecimal conducted = new BigDecimal("50");
        BigDecimal target = new BigDecimal("100");

        int minReq = shortageCalc.computeMinimumUnitsRequired(attended, conducted, target);
        assertThat(minReq).isEqualTo(-1);

        var result = new SubjectAttendanceResult(
            STUDENT_1, theorySubject().id(), POLICY_100, 1, SEMESTER_1,
            conducted, attended, new BigDecimal("98.00"), target,
            AttendanceClassification.SHORTAGE, new BigDecimal("1.00"), BigDecimal.ZERO, Instant.now());

        PredictionResult prediction = predictionEngine.predict(STUDENT_1, theorySubject().id(), result, 50);
        assertThat(prediction.canReachTarget()).isFalse();
    }

    @Test
    @DisplayName("MA-05: Boundary: Exactly at threshold has 0.00 shortage and 0.00 surplus")
    void exactlyAtThreshold_zeroShortageAndZeroSurplus() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID(), s4 = UUID.randomUUID();
        var sessions = List.of(
            conductedTheorySession(s1, semDay(1)),
            conductedTheorySession(s2, semDay(2)),
            conductedTheorySession(s3, semDay(3)),
            conductedTheorySession(s4, semDay(4))
        );
        // 3 present / 4 conducted = 75.00% exactly
        var records = List.of(
            record(s1, STUDENT_1, AttendanceStatus.PRESENT),
            record(s2, STUDENT_1, AttendanceStatus.PRESENT),
            record(s3, STUDENT_1, AttendanceStatus.PRESENT),
            record(s4, STUDENT_1, AttendanceStatus.ABSENT)
        );

        SubjectAttendanceResult result = subjectCalc.calculate(
            STUDENT_1, theorySubject(), policy75(), sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1);

        assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
        assertThat(result.shortageUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.surplusUnits()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("MA-06: Fractional repeating contributions (e.g. 1/3 = 0.3333333333) compute without exception")
    void fractionalRepeatingContributions_safePrecision() {
        BigDecimal oneThird = BigDecimal.ONE.divide(new BigDecimal("3"), 10, java.math.RoundingMode.HALF_UP);
        Map<AttendanceStatus, BigDecimal> contributions = Map.of(
            AttendanceStatus.PRESENT, BigDecimal.ONE,
            AttendanceStatus.ABSENT, BigDecimal.ZERO,
            AttendanceStatus.MEDICAL_LEAVE, oneThird
        );

        AttendancePolicy policy = AttendancePolicy.withContributions(
            UUID.randomUUID(), "One Third Policy", 1, new BigDecimal("75"), contributions);

        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID();
        var sessions = List.of(
            conductedTheorySession(s1, semDay(1)),
            conductedTheorySession(s2, semDay(2)),
            conductedTheorySession(s3, semDay(3))
        );
        var records = List.of(
            record(s1, STUDENT_1, AttendanceStatus.MEDICAL_LEAVE),
            record(s2, STUDENT_1, AttendanceStatus.MEDICAL_LEAVE),
            record(s3, STUDENT_1, AttendanceStatus.MEDICAL_LEAVE)
        );

        SubjectAttendanceResult result = subjectCalc.calculate(
            STUDENT_1, theorySubject(), policy, sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1);

        // 3 sessions * 1/3 unit = ~1.00 attended out of 3.00 conducted = 33.33%
        assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("MA-07: Policy rejection of negative and out-of-bounds thresholds")
    void invalidPolicyThresholds_rejected() {
        assertThatThrownBy(() -> AttendancePolicy.withDefaultContributions(
            UUID.randomUUID(), "Negative", new BigDecimal("-0.01")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> AttendancePolicy.withDefaultContributions(
            UUID.randomUUID(), "Above 100", new BigDecimal("100.01")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
