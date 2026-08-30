package com.amcs.infrastructure.persistence;

import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
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

@DisplayName("Phase 5.7: Report Adversarial, Boundary & Calculation Integration Tests")
class ReportAdversarialAndBoundaryIntegrationTest extends PostgresIntegrationTestBase {

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

    @BeforeEach
    @Transactional
    void setUp() {
        deptId = UUID.randomUUID();
        departmentRepo.save(new DepartmentEntity(deptId, "CSE_" + UUID.randomUUID().toString().substring(0, 6), "Computer Science"));

        periodId = UUID.randomUUID();
        periodRepo.save(new AcademicPeriodEntity(
            periodId, "Fall 2026_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20)
        ));

        sectionId = UUID.randomUUID();
        sectionRepo.save(new SectionEntity(sectionId, "Sec-A", deptId, periodId));

        subjectId = UUID.randomUUID();
        subjectRepo.save(new SubjectEntity(
            subjectId, "CS301_" + UUID.randomUUID().toString().substring(0, 6), "Algorithms", "THEORY", 4, deptId
        ));

        facultyId = UUID.randomUUID();
        facultyRepo.save(new FacultyEntity(
            facultyId, "EMP_ADV_" + UUID.randomUUID().toString().substring(0, 6), "Prof. Ada Lovelace", "ada@univ.edu", deptId
        ));
    }

    @Test
    @Transactional
    @DisplayName("Boundary: Zero sessions conducted handles cleanly with 100% and no division by zero")
    void zeroConductedSessionsHandledCleanly() {
        UUID studentId = UUID.randomUUID();
        studentRepo.save(new StudentEntity(studentId, "REG_ZERO", "Zero Student", "zero@univ.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), studentId, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        // No sessions created
        List<StudentAttendanceReportRow> rows = adapter.getStudentAttendanceReport(studentId, periodId, sectionId);
        assertThat(rows).isEmpty(); // No conducted sessions
    }

    @Test
    @Transactional
    @DisplayName("Boundary: 0% and 100% attendance calculations")
    void zeroAndHundredPercentCalculations() {
        UUID perfectStudentId = UUID.randomUUID();
        studentRepo.save(new StudentEntity(perfectStudentId, "REG_PERFECT", "Perfect", "p@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), perfectStudentId, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        UUID absentStudentId = UUID.randomUUID();
        studentRepo.save(new StudentEntity(absentStudentId, "REG_ABSENT", "Absent", "a@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), absentStudentId, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        // Conduct 4 sessions
        for (int i = 1; i <= 4; i++) {
            UUID sId = UUID.randomUUID();
            sessionRepo.save(new SessionEntity(
                sId, subjectId, sectionId, facultyId, periodId,
                LocalDate.of(2026, 9, i), "THEORY", 1, 1, "CONDUCTED", null, null
            ));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, perfectStudentId, "PRESENT"));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, absentStudentId, "ABSENT"));
        }

        List<StudentAttendanceReportRow> perfectRows = adapter.getStudentAttendanceReport(perfectStudentId, periodId, sectionId);
        assertThat(perfectRows).hasSize(1);
        assertThat(perfectRows.get(0).percentage()).isEqualByComparingTo("100.00");
        assertThat(perfectRows.get(0).status()).isEqualTo("ELIGIBLE");

        List<StudentAttendanceReportRow> absentRows = adapter.getStudentAttendanceReport(absentStudentId, periodId, sectionId);
        assertThat(absentRows).hasSize(1);
        assertThat(absentRows.get(0).percentage()).isEqualByComparingTo("0.00");
        assertThat(absentRows.get(0).status()).isEqualTo("DEFAULTER");
    }

    @Test
    @Transactional
    @DisplayName("Boundary: Threshold edges (75.00% is ELIGIBLE, 74.99% or below is DEFAULTER)")
    void thresholdBoundaryEdges() {
        // Conduct 100 sessions:
        // Student A: 75 attended out of 100 = 75.00% -> ELIGIBLE (not defaulter)
        // Student B: 74 attended out of 100 = 74.00% -> DEFAULTER
        UUID student75 = UUID.randomUUID();
        studentRepo.save(new StudentEntity(student75, "REG_75", "Student 75", "s75@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), student75, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        UUID student74 = UUID.randomUUID();
        studentRepo.save(new StudentEntity(student74, "REG_74", "Student 74", "s74@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), student74, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        for (int i = 1; i <= 100; i++) {
            UUID sId = UUID.randomUUID();
            sessionRepo.save(new SessionEntity(
                sId, subjectId, sectionId, facultyId, periodId,
                LocalDate.of(2026, 9, 1).plusDays(i), "THEORY", 1, 1, "CONDUCTED", null, null
            ));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, student75, i <= 75 ? "PRESENT" : "ABSENT"));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, student74, i <= 74 ? "PRESENT" : "ABSENT"));
        }

        List<DefaulterReportRow> defaulters = adapter.getDefaulterReport(periodId, sectionId, subjectId, new BigDecimal("75.00"));
        assertThat(defaulters).hasSize(1);
        assertThat(defaulters.get(0).studentName()).isEqualTo("Student 74");
        assertThat(defaulters.get(0).currentPercentage()).isEqualByComparingTo("74.00");
        assertThat(defaulters.get(0).unitsShort()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("RPT-008: Prediction states (ALREADY_MET, ACHIEVABLE, MATHEMATICALLY_IMPOSSIBLE)")
    void predictionStatesVerification() {
        // Conduct 20 sessions:
        // Student A has 20/20 (100%) -> with 2 future sessions, threshold 75% of 22 is 17 -> ALREADY_MET
        // Student B has 10/20 (50%) -> with 2 future sessions, threshold 75% of 22 is 17 -> needs 7 > 2 -> MATHEMATICALLY_IMPOSSIBLE
        // Student C has 16/20 (80%) -> with 2 future sessions, threshold 75% of 22 is 17 -> needs 1 <= 2 -> ACHIEVABLE
        UUID stA = UUID.randomUUID();
        studentRepo.save(new StudentEntity(stA, "REG_A", "Student A", "a@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), stA, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        UUID stB = UUID.randomUUID();
        studentRepo.save(new StudentEntity(stB, "REG_B", "Student B", "b@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), stB, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        UUID stC = UUID.randomUUID();
        studentRepo.save(new StudentEntity(stC, "REG_C", "Student C", "c@u.edu", deptId));
        enrollmentRepo.save(new EnrollmentEntity(UUID.randomUUID(), stC, sectionId, LocalDate.of(2026, 8, 1), null, "ACTIVE"));

        for (int i = 1; i <= 20; i++) {
            UUID sId = UUID.randomUUID();
            sessionRepo.save(new SessionEntity(
                sId, subjectId, sectionId, facultyId, periodId,
                LocalDate.of(2026, 9, 1).plusDays(i), "THEORY", 1, 1, "CONDUCTED", null, null
            ));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, stA, "PRESENT"));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, stB, i <= 10 ? "PRESENT" : "ABSENT"));
            recordRepo.save(new AttendanceRecordEntity(UUID.randomUUID(), sId, stC, i <= 16 ? "PRESENT" : "ABSENT"));
        }

        List<PredictionReportRow> predictions = adapter.getPredictionReport(subjectId, sectionId, periodId, 2, new BigDecimal("75.00"));
        assertThat(predictions).hasSize(3);

        PredictionReportRow predA = predictions.stream().filter(p -> p.studentName().equals("Student A")).findFirst().orElseThrow();
        assertThat(predA.feasibilityStatus()).isEqualTo("ALREADY_MET");
        assertThat(predA.requiredFutureSessions()).isEqualTo(0);

        PredictionReportRow predB = predictions.stream().filter(p -> p.studentName().equals("Student B")).findFirst().orElseThrow();
        assertThat(predB.feasibilityStatus()).isEqualTo("MATHEMATICALLY_IMPOSSIBLE");
        assertThat(predB.requiredFutureSessions()).isEqualTo(7);

        PredictionReportRow predC = predictions.stream().filter(p -> p.studentName().equals("Student C")).findFirst().orElseThrow();
        assertThat(predC.feasibilityStatus()).isEqualTo("ACHIEVABLE");
        assertThat(predC.requiredFutureSessions()).isEqualTo(1);
    }
}
