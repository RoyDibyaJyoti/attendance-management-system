package com.amcs.domain.calculation;

import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.calculation.result.OverallAttendanceResult;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.policy.OverallAggregationStrategy;
import com.amcs.domain.policy.OverallAttendancePolicy;
import com.amcs.domain.test.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amcs.domain.test.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("OverallAttendanceCalculator")
class OverallAttendanceCalculatorTest {

    private OverallAttendanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OverallAttendanceCalculator();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SubjectAttendanceResult makeResult(UUID subjectId, int attended, int conducted) {
        BigDecimal a = BigDecimal.valueOf(attended);
        BigDecimal c = BigDecimal.valueOf(conducted);
        BigDecimal pct = c.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : a.multiply(new BigDecimal("100")).divide(c, 2, java.math.RoundingMode.HALF_UP);
        AttendanceClassification cls = c.compareTo(BigDecimal.ZERO) == 0
            ? AttendanceClassification.UNDEFINED
            : (pct.compareTo(new BigDecimal("75")) >= 0
                ? AttendanceClassification.ADEQUATE : AttendanceClassification.SHORTAGE);

        return new SubjectAttendanceResult(
            STUDENT_1, subjectId, POLICY_75, 1, SEMESTER_1,
            c, a, pct, new BigDecimal("75"), cls,
            BigDecimal.ZERO, BigDecimal.ZERO, Instant.now()
        );
    }

    private UUID sub(int n) {
        return UUID.fromString("40000000-0000-0000-0000-0000000000" + String.format("%02d", n));
    }

    // =========================================================================
    // OA-A. ARITHMETIC_MEAN
    // =========================================================================

    @Nested
    @DisplayName("OA-A. ARITHMETIC_MEAN strategy")
    class ArithmeticMean {

        @Test
        @DisplayName("OA-A-01: Two subjects (80%, 90%) → mean = 85.00%")
        void twoSubjects_correctMean() {
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 16, 20), // 80%
                makeResult(s2, 18, 20)  // 90%
            );
            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Overall Mean 75",
                OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, Map.of(), policy, SEMESTER_1);

            assertThat(overall.overallPercentage()).isEqualByComparingTo(new BigDecimal("85.00"));
            assertThat(overall.isShortage()).isFalse();
        }

        @Test
        @DisplayName("OA-A-02: Arithmetic mean ≠ aggregate units (counter-example)")
        void arithmeticMean_notEqualToAggregate() {
            // Subject A: 15/20 = 75%; Subject B: 4/4 = 100%
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 15, 20), // 75%
                makeResult(s2, 4, 4)    // 100%
            );
            var meanPolicy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Mean",
                OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));
            var aggPolicy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Agg",
                OverallAggregationStrategy.AGGREGATE_UNITS, new BigDecimal("75"));

            var meanResult = calculator.calculate(STUDENT_1, results, Map.of(), meanPolicy, SEMESTER_1);
            var aggResult = calculator.calculate(STUDENT_1, results, Map.of(), aggPolicy, SEMESTER_1);

            // Mean = (75 + 100) / 2 = 87.50%
            assertThat(meanResult.overallPercentage()).isEqualByComparingTo(new BigDecimal("87.50"));
            // Agg = (15+4)/(20+4) = 19/24 = 79.17%
            assertThat(aggResult.overallPercentage()).isEqualByComparingTo(new BigDecimal("79.17"));
            // They must differ in this case
            assertThat(meanResult.overallPercentage()).isNotEqualByComparingTo(aggResult.overallPercentage());
        }
    }

    // =========================================================================
    // OA-B. AGGREGATE_UNITS
    // =========================================================================

    @Nested
    @DisplayName("OA-B. AGGREGATE_UNITS strategy")
    class AggregateUnits {

        @Test
        @DisplayName("OA-B-01: Two subjects — aggregate formula correct")
        void aggregateUnits_correct() {
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 10, 20), // 50%
                makeResult(s2, 18, 20)  // 90%
            );
            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Agg",
                OverallAggregationStrategy.AGGREGATE_UNITS, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, Map.of(), policy, SEMESTER_1);

            // 28/40 = 70.00% < 75% → shortage
            assertThat(overall.overallPercentage()).isEqualByComparingTo(new BigDecimal("70.00"));
            assertThat(overall.isShortage()).isTrue();
        }
    }

    // =========================================================================
    // OA-C. WEIGHTED_BY_CREDITS
    // =========================================================================

    @Nested
    @DisplayName("OA-C. WEIGHTED_BY_CREDITS strategy")
    class WeightedByCredits {

        @Test
        @DisplayName("OA-C-01: Two subjects with different credit hours")
        void weightedByCredits_correct() {
            // Subject A: 50%, 2 credit hours
            // Subject B: 90%, 4 credit hours
            // Weighted: (50×2 + 90×4) / (2+4) = 460/6 = 76.67%
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 10, 20), // 50%
                makeResult(s2, 18, 20)  // 90%
            );
            var creditMap = Map.of(s1, 2, s2, 4);
            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Weighted Credits",
                OverallAggregationStrategy.WEIGHTED_BY_CREDITS, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, creditMap, policy, SEMESTER_1);

            assertThat(overall.overallPercentage()).isEqualByComparingTo(new BigDecimal("76.67"));
            assertThat(overall.isShortage()).isFalse();
        }

        @Test
        @DisplayName("OA-C-02: Subjects with 0 credit hours excluded from weighted calculation")
        void zeroCredits_subjectExcluded() {
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 10, 20), // 50% — creditHours=0, excluded
                makeResult(s2, 18, 20)  // 90% — creditHours=4
            );
            var creditMap = Map.of(s1, 0, s2, 4); // s1 has 0 credits
            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Weighted",
                OverallAggregationStrategy.WEIGHTED_BY_CREDITS, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, creditMap, policy, SEMESTER_1);

            // Only s2 counts: 90.00%
            assertThat(overall.overallPercentage()).isEqualByComparingTo(new BigDecimal("90.00"));
        }
    }

    // =========================================================================
    // OA-D. UNDEFINED exclusion
    // =========================================================================

    @Nested
    @DisplayName("OA-D. UNDEFINED subject exclusion")
    class UndefinedExclusion {

        @Test
        @DisplayName("OA-D-01: UNDEFINED subjects excluded from aggregation")
        void undefinedSubjects_excluded() {
            UUID s1 = sub(1), s2 = sub(2);
            var results = List.of(
                makeResult(s1, 18, 20),  // 90% — eligible
                makeResult(s2, 0, 0)     // UNDEFINED — excluded
            );
            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Mean",
                OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, Map.of(), policy, SEMESTER_1);

            // Should be 90%, not (90 + 0) / 2 = 45%
            assertThat(overall.overallPercentage()).isEqualByComparingTo(new BigDecimal("90.00"));
        }

        @Test
        @DisplayName("OA-D-02: All UNDEFINED → 0% overall (no subjects conducted)")
        void allUndefined_zeroPercent() {
            UUID s1 = sub(1);
            var results = List.of(makeResult(s1, 0, 0)); // UNDEFINED

            var policy = OverallAttendancePolicy.of(
                POLICY_OVERALL, "Mean",
                OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));

            var overall = calculator.calculate(STUDENT_1, results, Map.of(), policy, SEMESTER_1);

            assertThat(overall.overallPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("OA-D-03: Empty subject list → 0% overall")
        void emptySubjectList_zeroPercent() {
            var policy = TestFixtures.overallMean75();
            var overall = calculator.calculate(STUDENT_1, List.of(), Map.of(), policy, SEMESTER_1);
            assertThat(overall.overallPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // OA-E. Policy metadata
    // =========================================================================

    @Nested
    @DisplayName("OA-E. Policy metadata capture")
    class PolicyMetadataCapture {

        @Test
        @DisplayName("OA-E-01: Strategy captured in result")
        void strategy_capturedInResult() {
            var results = List.of(makeResult(sub(1), 16, 20));
            var policy = TestFixtures.overallAggregate75();
            var overall = calculator.calculate(STUDENT_1, results, Map.of(), policy, SEMESTER_1);
            assertThat(overall.strategy()).isEqualTo(OverallAggregationStrategy.AGGREGATE_UNITS);
        }
    }

    // =========================================================================
    // OA-F. Distinctness across all 4 strategies
    // =========================================================================

    @Nested
    @DisplayName("OA-F. Distinctness of all four strategies")
    class AllStrategiesDistinctness {

        @Test
        @DisplayName("OA-F-01: All 4 strategies produce mathematically distinct percentages on same dataset")
        void allFourStrategies_produceDistinctResults() {
            // Subject 1: Theory (2 credits, 1 hr/unit, 10 attended / 20 conducted = 50.00%)
            // Subject 2: Lab    (4 credits, 3 hrs/unit,  9 attended / 10 conducted = 90.00%)
            UUID s1 = sub(1);
            UUID s2 = sub(2);

            var results = List.of(
                makeResult(s1, 10, 20), // 50.00%
                makeResult(s2, 9, 10)   // 90.00%
            );
            var creditMap = Map.of(s1, 2, s2, 4);
            var hourMap = Map.of(s1, new BigDecimal("1.0"), s2, new BigDecimal("3.0"));

            var pMean = OverallAttendancePolicy.of(UUID.randomUUID(), "Mean",
                OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));
            var pCred = OverallAttendancePolicy.of(UUID.randomUUID(), "Cred",
                OverallAggregationStrategy.WEIGHTED_BY_CREDITS, new BigDecimal("75"));
            var pUnit = OverallAttendancePolicy.of(UUID.randomUUID(), "Unit",
                OverallAggregationStrategy.AGGREGATE_UNITS, new BigDecimal("75"));
            var pHour = OverallAttendancePolicy.of(UUID.randomUUID(), "Hour",
                OverallAggregationStrategy.AGGREGATE_HOURS, new BigDecimal("75"));

            BigDecimal rMean = calculator.calculate(STUDENT_1, results, creditMap, hourMap, pMean, SEMESTER_1).overallPercentage();
            BigDecimal rCred = calculator.calculate(STUDENT_1, results, creditMap, hourMap, pCred, SEMESTER_1).overallPercentage();
            BigDecimal rUnit = calculator.calculate(STUDENT_1, results, creditMap, hourMap, pUnit, SEMESTER_1).overallPercentage();
            BigDecimal rHour = calculator.calculate(STUDENT_1, results, creditMap, hourMap, pHour, SEMESTER_1).overallPercentage();

            // 1. ARITHMETIC_MEAN: (50.00 + 90.00) / 2 = 70.00%
            assertThat(rMean).isEqualByComparingTo(new BigDecimal("70.00"));

            // 2. WEIGHTED_BY_CREDITS: (50×2 + 90×4) / (2+4) = 460/6 = 76.67%
            assertThat(rCred).isEqualByComparingTo(new BigDecimal("76.67"));

            // 3. AGGREGATE_UNITS: (10 + 9) / (20 + 10) = 19/30 = 63.33%
            assertThat(rUnit).isEqualByComparingTo(new BigDecimal("63.33"));

            // 4. AGGREGATE_HOURS: (10×1 + 9×3) / (20×1 + 10×3) = 37/50 = 74.00%
            assertThat(rHour).isEqualByComparingTo(new BigDecimal("74.00"));

            // Verify they are all distinct from one another:
            assertThat(rMean).isNotEqualByComparingTo(rCred);
            assertThat(rMean).isNotEqualByComparingTo(rUnit);
            assertThat(rMean).isNotEqualByComparingTo(rHour);
            assertThat(rCred).isNotEqualByComparingTo(rUnit);
            assertThat(rCred).isNotEqualByComparingTo(rHour);
            assertThat(rUnit).isNotEqualByComparingTo(rHour);
        }
    }
}
