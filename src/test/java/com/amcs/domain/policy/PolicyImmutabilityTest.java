package com.amcs.domain.policy;

import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.calculation.SubjectAttendanceCalculator;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.test.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Policy Immutability & Reproducibility Tests")
class PolicyImmutabilityTest {

    @Test
    @DisplayName("PI-01: statusContributions map is truly unmodifiable after policy creation")
    void statusContributions_cannotBeMutatedExternally() {
        AttendancePolicy policy = TestFixtures.policy75();

        assertThatThrownBy(() -> policy.statusContributions().put(AttendanceStatus.ABSENT, BigDecimal.ONE))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> policy.statusContributions().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("PI-02: Mutating source map after policy creation has no effect on policy")
    void mutatingSourceMap_doesNotAffectPolicy() {
        Map<AttendanceStatus, BigDecimal> sourceMap = new EnumMap<>(AttendanceStatus.class);
        sourceMap.put(AttendanceStatus.PRESENT, BigDecimal.ONE);
        sourceMap.put(AttendanceStatus.ABSENT, BigDecimal.ZERO);

        AttendancePolicy policy = AttendancePolicy.withContributions(
            UUID.randomUUID(), "Test Policy", 1, new BigDecimal("75"), sourceMap);

        // Mutate source map
        sourceMap.put(AttendanceStatus.ABSENT, BigDecimal.ONE);

        // Policy must retain original contribution
        assertThat(policy.getContribution(AttendanceStatus.ABSENT))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("PI-03: Historical reproducibility — Policy v1 results remain deterministic when v2 is introduced")
    void historicalCalculation_reproducibleAcrossPolicyVersions() {
        SubjectAttendanceCalculator calculator = new SubjectAttendanceCalculator();

        // 16 present out of 20 conducted = 80.00%
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID(), s4 = UUID.randomUUID();
        var sessions = List.of(
            conductedTheorySession(s1, semDay(1)),
            conductedTheorySession(s2, semDay(2)),
            conductedTheorySession(s3, semDay(3)),
            conductedTheorySession(s4, semDay(4))
        );
        // 3 present, 1 absent = 75.00%
        var records = List.of(
            record(s1, STUDENT_1, AttendanceStatus.PRESENT),
            record(s2, STUDENT_1, AttendanceStatus.PRESENT),
            record(s3, STUDENT_1, AttendanceStatus.PRESENT),
            record(s4, STUDENT_1, AttendanceStatus.ABSENT)
        );

        UUID policyId = UUID.randomUUID();

        // Version 1: 75% threshold
        AttendancePolicy policyV1 = AttendancePolicy.withContributions(
            policyId, "Standard Policy", 1, new BigDecimal("75"),
            Map.of(AttendanceStatus.PRESENT, BigDecimal.ONE, AttendanceStatus.ABSENT, BigDecimal.ZERO));

        // Version 2: 80% threshold introduced later
        AttendancePolicy policyV2 = AttendancePolicy.withContributions(
            policyId, "Standard Policy", 2, new BigDecimal("80"),
            Map.of(AttendanceStatus.PRESENT, BigDecimal.ONE, AttendanceStatus.ABSENT, BigDecimal.ZERO));

        // Historical calculation under Policy v1
        SubjectAttendanceResult resultV1 = calculator.calculate(
            STUDENT_1, theorySubject(), policyV1, sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1);

        // Calculation under Policy v2
        SubjectAttendanceResult resultV2 = calculator.calculate(
            STUDENT_1, theorySubject(), policyV2, sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1);

        // Recomputation under Policy v1 at a later date
        SubjectAttendanceResult recomputedV1 = calculator.calculate(
            STUDENT_1, theorySubject(), policyV1, sessions, records,
            fullEnrollment(STUDENT_1), SEMESTER_1);

        // 1. Result v1 is ADEQUATE (75% >= 75%)
        assertThat(resultV1.policyVersion()).isEqualTo(1);
        assertThat(resultV1.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
        assertThat(resultV1.isAdequate()).isTrue();

        // 2. Result v2 is SHORTAGE (75% < 80%)
        assertThat(resultV2.policyVersion()).isEqualTo(2);
        assertThat(resultV2.classification()).isEqualTo(AttendanceClassification.SHORTAGE);
        assertThat(resultV2.isShortage()).isTrue();

        // 3. Recomputed V1 is IDENTICAL to historical result V1
        assertThat(recomputedV1.attendancePercentage()).isEqualByComparingTo(resultV1.attendancePercentage());
        assertThat(recomputedV1.conductedUnits()).isEqualByComparingTo(resultV1.conductedUnits());
        assertThat(recomputedV1.attendedUnits()).isEqualByComparingTo(resultV1.attendedUnits());
        assertThat(recomputedV1.classification()).isEqualTo(resultV1.classification());
        assertThat(recomputedV1.policyVersion()).isEqualTo(resultV1.policyVersion());
    }
}
