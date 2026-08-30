package com.amcs.infrastructure.persistence;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import com.amcs.infrastructure.persistence.adapter.AttendanceReportPersistenceAdapter;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.AttendanceRecordEntity;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.EnrollmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SessionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataAttendanceRecordRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataEnrollmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSessionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Attendance Report Persistence Integration Tests")
class AttendanceReportPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired private AttendanceReportPersistenceAdapter adapter;
    @Autowired private SpringDataDepartmentRepository departmentRepo;
    @Autowired private SpringDataAcademicPeriodRepository periodRepo;
    @Autowired private SpringDataSectionRepository sectionRepo;
    @Autowired private SpringDataSubjectRepository subjectRepo;
    @Autowired private SpringDataFacultyRepository facultyRepo;
    @Autowired private SpringDataStudentRepository studentRepo;
    @Autowired private SpringDataEnrollmentRepository enrollmentRepo;
    @Autowired private SpringDataSessionRepository sessionRepo;
    @Autowired private SpringDataAttendanceRecordRepository recordRepo;

    private UUID deptId;
    private UUID periodId;
    private UUID sectionId;
    private UUID subjectId;
    private UUID facultyId;
    private UUID student1Id;
    private UUID student2Id;
    private UUID session1Id;
    private UUID session2Id;

    @BeforeEach
    @Transactional
    void setUpTestData() {
        deptId = UUID.randomUUID();
        departmentRepo.save(new DepartmentEntity(deptId, "CSE_" + UUID.randomUUID().toString().substring(0, 6), "Computer Science"));

        periodId = UUID.randomUUID();
        periodRepo.save(new AcademicPeriodEntity(
            periodId, "Fall 2026", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20)
        ));

        sectionId = UUID.randomUUID();
        sectionRepo.save(new SectionEntity(sectionId, "Sec-A", deptId, periodId));

        subjectId = UUID.randomUUID();
        subjectRepo.save(new SubjectEntity(
            subjectId, "CS101_" + UUID.randomUUID().toString().substring(0, 6), "Programming Basics", "THEORY", 4, deptId
        ));

        facultyId = UUID.randomUUID();
        facultyRepo.save(new FacultyEntity(
            facultyId, "EMP_" + UUID.randomUUID().toString().substring(0, 6), "Prof. Alan Turing", "turing@univ.edu", deptId
        ));

        student1Id = UUID.randomUUID();
        studentRepo.save(new StudentEntity(
            student1Id, "REG_01_" + UUID.randomUUID().toString().substring(0, 6), "Alice", "alice@univ.edu", deptId
        ));
        enrollmentRepo.save(new EnrollmentEntity(
            UUID.randomUUID(), student1Id, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"
        ));

        student2Id = UUID.randomUUID();
        studentRepo.save(new StudentEntity(
            student2Id, "REG_02_" + UUID.randomUUID().toString().substring(0, 6), "Bob", "bob@univ.edu", deptId
        ));
        enrollmentRepo.save(new EnrollmentEntity(
            UUID.randomUUID(), student2Id, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"
        ));

        // Create 2 sessions: 1 on 2026-09-01, 1 on 2026-09-02
        session1Id = UUID.randomUUID();
        sessionRepo.save(new SessionEntity(
            session1Id, subjectId, sectionId, facultyId, periodId,
            LocalDate.of(2026, 9, 1), "THEORY", 1, 1, "CONDUCTED", null, null
        ));

        session2Id = UUID.randomUUID();
        sessionRepo.save(new SessionEntity(
            session2Id, subjectId, sectionId, facultyId, periodId,
            LocalDate.of(2026, 9, 2), "THEORY", 1, 1, "CONDUCTED", null, null
        ));

        // Student 1: PRESENT in session 1, PRESENT in session 2 (100%)
        recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), session1Id, student1Id, "PRESENT"));
        recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), session2Id, student1Id, "PRESENT"));

        // Student 2: ABSENT in session 1, DUTY_LEAVE in session 2 (50% or duty leave attended)
        recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), session1Id, student2Id, "ABSENT"));
        recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), session2Id, student2Id, "DUTY_LEAVE"));
    }

    @Test
    @Transactional
    @DisplayName("RPT-001: getStudentAttendanceReport retrieves aggregated student subject attendance")
    void testGetStudentAttendanceReport() {
        List<StudentAttendanceReportRow> rows = adapter.getStudentAttendanceReport(student1Id, periodId, sectionId);
        assertThat(rows).hasSize(1);
        StudentAttendanceReportRow row = rows.get(0);
        assertThat(row.subjectId()).isEqualTo(subjectId);
        assertThat(row.conductedUnits()).isEqualTo(2);
        assertThat(row.attendedUnits()).isEqualTo(2);
        assertThat(row.percentage()).isEqualByComparingTo("100.00");
        assertThat(row.status()).isEqualTo("ELIGIBLE");
    }

    @Test
    @Transactional
    @DisplayName("RPT-002: getSubjectAttendanceSummary retrieves section roster attendance")
    void testGetSubjectAttendanceSummary() {
        List<SubjectAttendanceSummaryReportRow> rows = adapter.getSubjectAttendanceSummary(subjectId, sectionId, periodId);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(SubjectAttendanceSummaryReportRow::studentName)
            .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @Transactional
    @DisplayName("RPT-003: getDefaulterReport detects students below threshold")
    void testGetDefaulterReport() {
        // Alice has 100%, Bob has 50% (DUTY_LEAVE is counted as attended: 1 attended out of 2 = 50%)
        // With threshold 75%, Bob should be flagged as defaulter
        List<DefaulterReportRow> defaulters = adapter.getDefaulterReport(periodId, sectionId, subjectId, new BigDecimal("75.00"));
        assertThat(defaulters).hasSize(1);
        assertThat(defaulters.get(0).studentName()).isEqualTo("Bob");
        assertThat(defaulters.get(0).currentPercentage()).isEqualByComparingTo("50.00");
        assertThat(defaulters.get(0).unitsShort()).isGreaterThan(0);
    }

    @Test
    @Transactional
    @DisplayName("RPT-004: getAttendanceRegisterReport creates chronological matrix")
    void testGetAttendanceRegisterReport() {
        AttendanceRegisterReportData register = adapter.getAttendanceRegisterReport(subjectId, sectionId, periodId, null, null);
        assertThat(register.sessionDates()).containsExactly(
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)
        );
        assertThat(register.studentRows()).hasSize(2);

        AttendanceRegisterReportData.StudentRegisterRow alice = register.studentRows().stream()
            .filter(r -> r.studentName().equals("Alice")).findFirst().orElseThrow();
        assertThat(alice.attendanceByDate().get(LocalDate.of(2026, 9, 1))).isEqualTo("PRESENT");
        assertThat(alice.attendanceByDate().get(LocalDate.of(2026, 9, 2))).isEqualTo("PRESENT");
        assertThat(alice.totalAttended()).isEqualTo(2);

        AttendanceRegisterReportData.StudentRegisterRow bob = register.studentRows().stream()
            .filter(r -> r.studentName().equals("Bob")).findFirst().orElseThrow();
        assertThat(bob.attendanceByDate().get(LocalDate.of(2026, 9, 1))).isEqualTo("ABSENT");
        assertThat(bob.attendanceByDate().get(LocalDate.of(2026, 9, 2))).isEqualTo("DUTY_LEAVE");
        assertThat(bob.totalAttended()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("RPT-005: getOverallAttendanceSummary computes section-wide aggregates")
    void testGetOverallAttendanceSummary() {
        List<OverallAttendanceReportRow> rows = adapter.getOverallAttendanceSummary(periodId, sectionId, null, new BigDecimal("75.00"));
        assertThat(rows).hasSize(2);
        OverallAttendanceReportRow alice = rows.stream().filter(r -> r.studentName().equals("Alice")).findFirst().orElseThrow();
        assertThat(alice.overallStatus()).isEqualTo("ELIGIBLE");
        OverallAttendanceReportRow bob = rows.stream().filter(r -> r.studentName().equals("Bob")).findFirst().orElseThrow();
        assertThat(bob.overallStatus()).isEqualTo("DEFAULTER");
    }

    @Test
    @Transactional
    @DisplayName("RPT-006: getFacultyComplianceReport tracks conducted vs scheduled sessions")
    void testGetFacultyComplianceReport() {
        List<FacultyComplianceReportRow> compliance = adapter.getFacultyComplianceReport(periodId, facultyId, null);
        assertThat(compliance).hasSize(1);
        FacultyComplianceReportRow r = compliance.get(0);
        assertThat(r.facultyName()).isEqualTo("Prof. Alan Turing");
        assertThat(r.conductedSessions()).isEqualTo(2);
        assertThat(r.unconductedSessions()).isEqualTo(0);
        assertThat(r.compliancePercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    @Transactional
    @DisplayName("RPT-007: getCondonationRegisterReport filters DUTY_LEAVE and MEDICAL_LEAVE")
    void testGetCondonationRegisterReport() {
        List<CondonationRegisterReportRow> condonations = adapter.getCondonationRegisterReport(
            periodId, sectionId, subjectId, "DUTY_LEAVE", null, null
        );
        assertThat(condonations).hasSize(1);
        assertThat(condonations.get(0).studentName()).isEqualTo("Bob");
        assertThat(condonations.get(0).leaveType()).isEqualTo("DUTY_LEAVE");
    }

    @Test
    @Transactional
    @DisplayName("RPT-008: getPredictionReport projects future session attendance")
    void testGetPredictionReport() {
        List<PredictionReportRow> predictions = adapter.getPredictionReport(subjectId, sectionId, periodId, 10, new BigDecimal("75.00"));
        assertThat(predictions).hasSize(2);

        PredictionReportRow alice = predictions.stream().filter(r -> r.studentName().equals("Alice")).findFirst().orElseThrow();
        // Alice current 2/2. Target: ceil((2 + 10) * 0.75) = 9. Current attended: 2. Required future: 7.
        assertThat(alice.requiredFutureSessions()).isEqualTo(7);
        assertThat(alice.feasibilityStatus()).isEqualTo("ACHIEVABLE");

        PredictionReportRow bob = predictions.stream().filter(r -> r.studentName().equals("Bob")).findFirst().orElseThrow();
        // Bob current 1/2. Target: ceil(12 * 0.75) = 9. Current attended: 1. Required future: 8.
        assertThat(bob.requiredFutureSessions()).isEqualTo(8);
        assertThat(bob.feasibilityStatus()).isEqualTo("ACHIEVABLE");
    }
}
