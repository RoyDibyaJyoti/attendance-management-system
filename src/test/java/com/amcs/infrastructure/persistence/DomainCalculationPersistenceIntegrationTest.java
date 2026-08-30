package com.amcs.infrastructure.persistence;

import com.amcs.application.service.AttendanceCalculationOrchestrator;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceClassification;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.calculation.result.SubjectAttendanceResult;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.adapter.AcademicPeriodPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.AttendancePolicyPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.AttendanceRecordPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.EnrollmentPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.SessionPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.StudentPersistenceAdapter;
import com.amcs.infrastructure.persistence.adapter.SubjectPersistenceAdapter;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Calculation Integration via Persistence Layer")
class DomainCalculationPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private AttendanceCalculationOrchestrator orchestrator;

    @Autowired
    private StudentPersistenceAdapter studentAdapter;

    @Autowired
    private SubjectPersistenceAdapter subjectAdapter;

    @Autowired
    private AcademicPeriodPersistenceAdapter periodAdapter;

    @Autowired
    private SessionPersistenceAdapter sessionAdapter;

    @Autowired
    private AttendanceRecordPersistenceAdapter recordAdapter;

    @Autowired
    private EnrollmentPersistenceAdapter enrollmentAdapter;

    @Autowired
    private AttendancePolicyPersistenceAdapter policyAdapter;

    @Autowired
    private SpringDataDepartmentRepository departmentRepository;

    @Autowired
    private SpringDataFacultyRepository facultyRepository;

    @Autowired
    private SpringDataSectionRepository sectionRepository;

    @Test
    @Transactional
    @DisplayName("DCP-01: End-to-end: Load persisted facts from PostgreSQL into pure calculation engine")
    void endToEndPersistedCalculation() {
        // 1. Setup Curricular Infrastructure
        DepartmentEntity dept = departmentRepository.save(
            new DepartmentEntity(UUID.randomUUID(), "CSE_" + UUID.randomUUID().toString().substring(0, 6), "Computer Science"));

        FacultyEntity faculty = facultyRepository.save(
            new FacultyEntity(UUID.randomUUID(), "FAC_" + UUID.randomUUID().toString().substring(0, 6), "Prof. Turing", "turing_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));

        UUID periodId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 15);
        AcademicPeriod domainPeriod = new AcademicPeriod("Fall 2026_" + UUID.randomUUID().toString().substring(0, 6), startDate, endDate);
        periodAdapter.save(periodId, domainPeriod);

        SectionEntity section = sectionRepository.save(
            new SectionEntity(UUID.randomUUID(), "Section A", dept.getId(), periodId));

        // 2. Setup Student and Enrollment
        UUID studentId = UUID.randomUUID();
        String regNo = "REG_DCP_" + UUID.randomUUID().toString().substring(0, 6);
        studentAdapter.save(new StudentEntity(studentId, regNo, "Ada Lovelace", regNo + "@test.edu", dept.getId()));

        Enrollment enrollment = new Enrollment(
            studentId, section.getId(), startDate, Optional.empty(), Optional.empty());
        enrollmentAdapter.save(enrollment);

        // 3. Setup Subject and Policy
        UUID subjectId = UUID.randomUUID();
        Subject subject = new Subject(
            subjectId, "Discrete Mathematics", "CS201_" + UUID.randomUUID().toString().substring(0, 6), CourseType.THEORY, 4);
        subjectAdapter.save(subject, dept.getId());

        UUID policyId = UUID.randomUUID();
        AttendancePolicy policy = new AttendancePolicy(
            policyId, "Math 75 Policy_" + UUID.randomUUID().toString().substring(0, 6), 1, new BigDecimal("75.00"),
            Map.of(AttendanceStatus.PRESENT, BigDecimal.ONE, AttendanceStatus.ABSENT, BigDecimal.ZERO),
            MissingRecordStrategy.TREAT_AS_ABSENT, Optional.empty(), Instant.now(), Optional.empty());
        policyAdapter.save(policy);

        // 4. Persist 20 Sessions and Records (16 Present, 4 Absent = 80.00%)
        List<Session> sessions = new ArrayList<>();
        List<AttendanceRecord> records = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            UUID sessionId = UUID.randomUUID();
            LocalDate sessionDate = startDate.plusDays(i);

            Session session = new Session(
                sessionId, subjectId, section.getId(), faculty.getId(), sessionDate,
                SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());
            sessionAdapter.save(session, periodId);
            sessions.add(session);

            AttendanceStatus status = (i <= 16) ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            AttendanceRecord record = new AttendanceRecord(UUID.randomUUID(), sessionId, studentId, status);
            records.add(record);
        }
        recordAdapter.saveAll(records);

        // 5. Execute Orchestrator: Loads persisted facts -> Maps to domain -> Runs pure engine
        SubjectAttendanceResult result = orchestrator.calculateSubjectAttendance(
            studentId, subjectId, section.getId(), policyId, periodId, domainPeriod);

        // 6. Verify result matches pure engine calculation
        assertThat(result.conductedUnits()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.attendedUnits()).isEqualByComparingTo(new BigDecimal("16.00"));
        assertThat(result.attendancePercentage()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(result.classification()).isEqualTo(AttendanceClassification.ADEQUATE);
        assertThat(result.isAdequate()).isTrue();
        assertThat(result.isShortage()).isFalse();
        assertThat(result.shortageUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.surplusUnits()).isGreaterThan(BigDecimal.ZERO);
    }
}
