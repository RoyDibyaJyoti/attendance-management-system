package com.amcs.domain.calculation;

import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.calculation.result.IntegratedSubjectAttendanceResult;
import com.amcs.domain.calculation.result.OverallAttendanceResult;
import com.amcs.domain.calculation.result.PredictionResult;
import com.amcs.domain.calculation.result.ShortageAnalysis;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.policy.IntegratedSubjectPolicy;
import com.amcs.domain.policy.OverallAggregationStrategy;
import com.amcs.domain.policy.OverallAttendancePolicy;
import com.amcs.domain.test.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AttendanceCalculationEngine Facade Tests")
class AttendanceCalculationEngineTest {

    private AttendanceCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AttendanceCalculationEngine();
    }

    @Test
    @DisplayName("Facade: calculateSubjectAttendance and analyzeShortage")
    void testSubjectAttendanceAndShortageAnalysis() {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        var sessions = List.of(
            conductedTheorySession(s1, semDay(1)),
            conductedTheorySession(s2, semDay(2))
        );
        var records = List.of(
            record(s1, STUDENT_1, AttendanceStatus.PRESENT),
            record(s2, STUDENT_1, AttendanceStatus.ABSENT)
        );

        SubjectAttendanceResult result = engine.calculateSubjectAttendance(
            STUDENT_1, theorySubject(), policy75(), sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1
        );

        assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.isShortage()).isTrue();

        ShortageAnalysis analysis = engine.analyzeShortage(result);
        assertThat(analysis.isInShortage()).isTrue();
        assertThat(analysis.unitsShort()).isGreaterThan(BigDecimal.ZERO);
        assertThat(analysis.unitsSurplus()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Facade: predictFutureAttendance")
    void testPrediction() {
        UUID s1 = UUID.randomUUID();
        var sessions = List.of(conductedTheorySession(s1, semDay(1)));
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        SubjectAttendanceResult result = engine.calculateSubjectAttendance(
            STUDENT_1, theorySubject(), policy75(), sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1
        );

        PredictionResult prediction = engine.predictFutureAttendance(
            STUDENT_1, theorySubject().id(), result, 10
        );

        assertThat(prediction.canReachTarget()).isTrue();
        assertThat(prediction.isAlreadyAboveTarget()).isTrue();
    }

    @Test
    @DisplayName("Facade: calculateIntegratedSubjectAttendance and calculateOverallAttendance")
    void testIntegratedAndOverall() {
        UUID s1 = UUID.randomUUID();
        var sessions = List.of(integratedTheorySession(s1, semDay(1)));
        var records = List.of(record(s1, STUDENT_1, AttendanceStatus.PRESENT));

        IntegratedSubjectAttendanceResult integratedResult = engine.calculateIntegratedSubjectAttendance(
            STUDENT_1, integratedSubject(),
            theoryComponent(policy75()), labComponent(policy80()),
            IntegratedSubjectPolicy.separateThresholds(),
            sessions, records, fullEnrollment(STUDENT_1), SEMESTER_1
        );

        assertThat(integratedResult.theoryComponent().result().isAdequate()).isTrue();

        OverallAttendancePolicy overallPolicy = OverallAttendancePolicy.of(
            UUID.randomUUID(), "Overall Mean", OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75")
        );

        OverallAttendanceResult overallResult = engine.calculateOverallAttendance(
            STUDENT_1,
            List.of(integratedResult.theoryComponent().result()),
            Map.of(),
            overallPolicy,
            SEMESTER_1
        );

        assertThat(overallResult.overallPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(overallResult.isShortage()).isFalse();
    }
}
