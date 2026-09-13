package com.amcs.domain.test;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.ComponentType;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.academic.SubjectComponent;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.OverallAttendancePolicy;
import com.amcs.domain.policy.OverallAggregationStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared test data factory for all unit tests.
 * Produces deterministic, immutable test objects.
 * All UUIDs are stable across test runs.
 */
public final class TestFixtures {

    // ── Fixed UUIDs ──────────────────────────────────────────────────────────
    public static final UUID STUDENT_1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID STUDENT_2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID SECTION_1 = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID FACULTY_1 = UUID.fromString("30000000-0000-0000-0000-000000000001");

    public static final UUID SUBJECT_THEORY = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID SUBJECT_LAB = UUID.fromString("40000000-0000-0000-0000-000000000002");
    public static final UUID SUBJECT_INTEGRATED = UUID.fromString("40000000-0000-0000-0000-000000000003");

    public static final UUID POLICY_75 = UUID.fromString("50000000-0000-0000-0000-000000000001");
    public static final UUID POLICY_80 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    public static final UUID POLICY_100 = UUID.fromString("50000000-0000-0000-0000-000000000003");
    public static final UUID POLICY_OVERALL = UUID.fromString("50000000-0000-0000-0000-000000000010");

    public static final UUID LAB_GROUP_A = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID LAB_GROUP_B = UUID.fromString("60000000-0000-0000-0000-000000000002");

    // ── Academic Period ──────────────────────────────────────────────────────
    public static final AcademicPeriod SEMESTER_1 = new AcademicPeriod(
        "2024 Odd Semester",
        LocalDate.of(2024, 7, 1),
        LocalDate.of(2024, 11, 30)
    );

    // ── Standard Policies ────────────────────────────────────────────────────

    /**
     * Standard 75% threshold policy.
     * PRESENT=1, ABSENT=0, DUTY_LEAVE=1, MEDICAL_LEAVE=1, ON_DUTY=1.
     *
     * NOTE: These contribution values are for test purposes only.
     * Production values REQUIRE INSTITUTIONAL CONFIRMATION.
     */
    public static AttendancePolicy policy75() {
        return AttendancePolicy.withDefaultContributions(POLICY_75, "Standard 75%", new BigDecimal("75"));
    }

    /** Standard 80% threshold policy. */
    public static AttendancePolicy policy80() {
        return AttendancePolicy.withDefaultContributions(POLICY_80, "Standard 80%", new BigDecimal("80"));
    }

    /** 100% threshold policy (lab maximum attendance). */
    public static AttendancePolicy policy100() {
        return AttendancePolicy.withDefaultContributions(POLICY_100, "100% Required", new BigDecimal("100"));
    }

    /**
     * Policy where DUTY_LEAVE counts as ABSENT (0) and MEDICAL_LEAVE counts as half (0.5).
     * For testing partial contributions.
     */
    public static AttendancePolicy policyWithPartialContributions(UUID id, BigDecimal threshold) {
        Map<AttendanceStatus, BigDecimal> c = new EnumMap<>(AttendanceStatus.class);
        c.put(AttendanceStatus.PRESENT, BigDecimal.ONE);
        c.put(AttendanceStatus.ABSENT, BigDecimal.ZERO);
        c.put(AttendanceStatus.DUTY_LEAVE, BigDecimal.ZERO);         // DL counts as absent
        c.put(AttendanceStatus.MEDICAL_LEAVE, new BigDecimal("0.5")); // ML counts as half
        c.put(AttendanceStatus.ON_DUTY, BigDecimal.ONE);
        return AttendancePolicy.withContributions(id, "Partial Contribution Policy", 1, threshold, c);
    }

    /** Overall 75% policy using arithmetic mean. */
    public static OverallAttendancePolicy overallMean75() {
        return OverallAttendancePolicy.of(POLICY_OVERALL, "Overall 75% Mean",
            OverallAggregationStrategy.ARITHMETIC_MEAN, new BigDecimal("75"));
    }

    /** Overall 75% policy using aggregate units. */
    public static OverallAttendancePolicy overallAggregate75() {
        return OverallAttendancePolicy.of(POLICY_OVERALL, "Overall 75% Aggregate",
            OverallAggregationStrategy.AGGREGATE_UNITS, new BigDecimal("75"));
    }

    // ── Subject factories ────────────────────────────────────────────────────

    public static Subject theorySubject() {
        return new Subject(SUBJECT_THEORY, "Engineering Mathematics", "MA101",
            CourseType.THEORY, 4, true);
    }

    public static Subject labSubject() {
        return new Subject(SUBJECT_LAB, "Physics Lab", "PH111L",
            CourseType.LABORATORY, 1, true);
    }

    public static Subject integratedSubject() {
        return new Subject(SUBJECT_INTEGRATED, "Electronics with Lab", "EC201",
            CourseType.THEORY_INTEGRATED_LABORATORY, 5, true);
    }

    // ── SubjectComponent factories ───────────────────────────────────────────

    public static SubjectComponent theoryComponent(AttendancePolicy policy) {
        return new SubjectComponent(SUBJECT_INTEGRATED, ComponentType.THEORY, policy);
    }

    public static SubjectComponent labComponent(AttendancePolicy policy) {
        return new SubjectComponent(SUBJECT_INTEGRATED, ComponentType.LAB, policy);
    }

    // ── Enrollment factories ─────────────────────────────────────────────────

    /**
     * Full-semester enrollment starting from semester start.
     */
    public static Enrollment fullEnrollment(UUID studentId) {
        return new Enrollment(studentId, SECTION_1,
            SEMESTER_1.startDate(), Optional.empty(), Optional.empty());
    }

    /**
     * Enrollment starting mid-period (for late-joiner tests).
     */
    public static Enrollment midEnrollment(UUID studentId, LocalDate start) {
        return new Enrollment(studentId, SECTION_1, start, Optional.empty(), Optional.empty());
    }

    /**
     * Enrollment for a student in Lab Group A.
     */
    public static Enrollment enrollmentWithLabGroup(UUID studentId, UUID labGroupId) {
        return new Enrollment(studentId, SECTION_1,
            SEMESTER_1.startDate(), Optional.empty(), Optional.of(labGroupId));
    }

    // ── Session factories ────────────────────────────────────────────────────

    public static Session conductedTheorySession(UUID sessionId, LocalDate date) {
        return new Session(sessionId, SUBJECT_THEORY, SECTION_1, FACULTY_1,
            date, SessionType.THEORY, 1, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
    }

    public static Session conductedTheorySession(UUID sessionId, LocalDate date, int units) {
        return new Session(sessionId, SUBJECT_THEORY, SECTION_1, FACULTY_1,
            date, SessionType.THEORY, units, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
    }

    public static Session conductedLabSession(UUID sessionId, LocalDate date, int units) {
        return new Session(sessionId, SUBJECT_LAB, SECTION_1, FACULTY_1,
            date, SessionType.LAB, units, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
    }

    public static Session conductedLabSessionForGroup(
        UUID sessionId, LocalDate date, int units, UUID labGroupId) {
        return new Session(sessionId, SUBJECT_LAB, SECTION_1, FACULTY_1,
            date, SessionType.LAB, units, SessionStatus.CONDUCTED,
            Optional.of(labGroupId), Optional.empty());
    }

    public static Session cancelledSession(UUID sessionId, LocalDate date) {
        return new Session(sessionId, SUBJECT_THEORY, SECTION_1, FACULTY_1,
            date, SessionType.THEORY, 1, SessionStatus.CANCELLED,
            Optional.empty(), Optional.empty());
    }

    public static Session scheduledSession(UUID sessionId, LocalDate date) {
        return new Session(sessionId, SUBJECT_THEORY, SECTION_1, FACULTY_1,
            date, SessionType.THEORY, 1, SessionStatus.SCHEDULED,
            Optional.empty(), Optional.empty());
    }

    /** Session for the integrated subject. */
    public static Session integratedTheorySession(UUID sessionId, LocalDate date) {
        return new Session(sessionId, SUBJECT_INTEGRATED, SECTION_1, FACULTY_1,
            date, SessionType.THEORY, 1, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
    }

    public static Session integratedLabSession(UUID sessionId, LocalDate date, int units) {
        return new Session(sessionId, SUBJECT_INTEGRATED, SECTION_1, FACULTY_1,
            date, SessionType.LAB, units, SessionStatus.CONDUCTED,
            Optional.empty(), Optional.empty());
    }

    // ── AttendanceRecord factories ───────────────────────────────────────────

    public static AttendanceRecord record(UUID sessionId, UUID studentId, AttendanceStatus status) {
        return new AttendanceRecord(UUID.randomUUID(), sessionId, studentId, status);
    }

    // ── Date helpers ─────────────────────────────────────────────────────────

    public static LocalDate semDay(int day) {
        return SEMESTER_1.startDate().plusDays(day - 1);
    }

    // Private constructor
    private TestFixtures() {}
}
