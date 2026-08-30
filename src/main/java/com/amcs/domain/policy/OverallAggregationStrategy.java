package com.amcs.domain.policy;

/**
 * Defines how individual subject-level attendance results are aggregated
 * into a single overall/general attendance figure for a student.
 *
 * <p><strong>CRITICAL: REQUIRES INSTITUTIONAL CONFIRMATION (RIC-OA-002).</strong>
 * The correct strategy MUST be confirmed before any overall attendance figure
 * is used for official purposes. Different strategies produce materially different results.
 *
 * <h3>Illustrative Example</h3>
 * <pre>
 *   Subject A: 10 attended / 20 conducted = 50%  (2 credit hours)
 *   Subject B: 18 attended / 20 conducted = 90%  (4 credit hours)
 *
 *   ARITHMETIC_MEAN:
 *     (50 + 90) / 2 = 70.00%
 *
 *   AGGREGATE_UNITS:
 *     (10 + 18) / (20 + 20) = 28/40 = 70.00%
 *     [same here, but NOT always the same as arithmetic mean]
 *
 *   WEIGHTED_BY_CREDITS:
 *     (50*2 + 90*4) / (2+4) = (100 + 360) / 6 = 460/6 = 76.67%
 *
 *   AGGREGATE_HOURS (if conductedUnits represent hours):
 *     Same formula as AGGREGATE_UNITS — semantically distinct but numerically identical
 *     if all sessions use hour-equivalent units.
 * </pre>
 *
 * <h3>Counter-example where ARITHMETIC_MEAN ≠ AGGREGATE_UNITS</h3>
 * <pre>
 *   Subject A: 15/20 = 75%
 *   Subject B:  4/ 4 = 100%
 *
 *   ARITHMETIC_MEAN:    (75 + 100) / 2    = 87.50%
 *   AGGREGATE_UNITS:    (15 + 4) / (20+4) = 19/24 = 79.17%
 * </pre>
 */
public enum OverallAggregationStrategy {

    /**
     * Overall = arithmetic mean of all subject-level attendance percentages.
     * Each subject has equal weight regardless of credit hours or session count.
     * Simple to explain but can be misleading when subjects have very different session counts.
     */
    ARITHMETIC_MEAN,

    /**
     * Overall = credit-weighted mean of subject-level attendance percentages.
     * Subjects with more credit hours have proportionally greater influence.
     * Requires valid credit hour data on each subject.
     * Requires institutional confirmation of credit hour values (RIC-OA-002).
     */
    WEIGHTED_BY_CREDITS,

    /**
     * Overall = (total attended units across all subjects) / (total conducted units across all subjects).
     * This is NOT a mean of percentages. It uses the raw unit counts from each subject.
     * Subjects with more sessions naturally carry greater weight.
     * Semantically: "of all the class-units I should have attended, what fraction did I attend?"
     */
    AGGREGATE_UNITS,

    /**
     * Same mathematical formula as AGGREGATE_UNITS, but the "unit" is explicitly
     * interpreted as an hour of instruction. The meaningful difference is in how
     * Session.conductedUnits is set (must represent hours).
     * REQUIRES INSTITUTIONAL CONFIRMATION (RIC-OA-002).
     */
    AGGREGATE_HOURS
}
