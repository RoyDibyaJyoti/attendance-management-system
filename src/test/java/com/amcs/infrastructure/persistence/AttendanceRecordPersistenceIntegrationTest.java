package com.amcs.infrastructure.persistence;

import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.AttendanceRecordEntity;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SessionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataAttendanceRecordRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSessionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSubjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Attendance Record Persistence Integration Tests")
class AttendanceRecordPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private SpringDataAttendanceRecordRepository recordRepository;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataSessionRepository sessionRepository;

    @Autowired
    private SpringDataDepartmentRepository departmentRepository;

    @Autowired
    private SpringDataFacultyRepository facultyRepository;

    @Autowired
    private SpringDataAcademicPeriodRepository periodRepository;

    @Autowired
    private SpringDataSectionRepository sectionRepository;

    @Autowired
    private SpringDataSubjectRepository subjectRepository;

    @Test
    @Transactional
    @DisplayName("AR-01: Insert and retrieve attendance record")
    void insertAndRetrieveAttendanceRecord() {
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(UUID.randomUUID(), "D1_" + UUID.randomUUID().toString().substring(0, 6), "Dept 1"));
        FacultyEntity fac = facultyRepository.save(new FacultyEntity(UUID.randomUUID(), "F1_" + UUID.randomUUID().toString().substring(0, 6), "Faculty", "f1_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));
        AcademicPeriodEntity period = periodRepository.save(new AcademicPeriodEntity(UUID.randomUUID(), "P1_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(3)));
        SectionEntity sec = sectionRepository.save(new SectionEntity(UUID.randomUUID(), "Sec1", dept.getId(), period.getId()));
        SubjectEntity sub = subjectRepository.save(new SubjectEntity(UUID.randomUUID(), "S1_" + UUID.randomUUID().toString().substring(0, 6), "Algorithms", "THEORY", 4, dept.getId()));
        StudentEntity student = studentRepository.save(new StudentEntity(UUID.randomUUID(), "REG_AR1_" + UUID.randomUUID().toString().substring(0, 6), "Bob", "bob_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));

        SessionEntity session = sessionRepository.save(new SessionEntity(
            UUID.randomUUID(), sub.getId(), sec.getId(), fac.getId(), period.getId(),
            LocalDate.now(), "THEORY", 1, 1, "CONDUCTED", null, null));

        UUID recordId = UUID.randomUUID();
        AttendanceRecordEntity record = new AttendanceRecordEntity(recordId, session.getId(), student.getId(), "PRESENT");
        recordRepository.save(record);

        var found = recordRepository.findById(recordId);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("PRESENT");
        assertThat(found.get().getSessionId()).isEqualTo(session.getId());
        assertThat(found.get().getStudentId()).isEqualTo(student.getId());
    }

    @Test
    @DisplayName("AR-02: Duplicate attendance record for same student and session is rejected by database unique constraint")
    void duplicateRecord_rejectedByDatabaseConstraint() {
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(UUID.randomUUID(), "D2_" + UUID.randomUUID().toString().substring(0, 6), "Dept 2"));
        FacultyEntity fac = facultyRepository.save(new FacultyEntity(UUID.randomUUID(), "F2_" + UUID.randomUUID().toString().substring(0, 6), "Faculty 2", "f2_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));
        AcademicPeriodEntity period = periodRepository.save(new AcademicPeriodEntity(UUID.randomUUID(), "P2_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(3)));
        SectionEntity sec = sectionRepository.save(new SectionEntity(UUID.randomUUID(), "Sec2", dept.getId(), period.getId()));
        SubjectEntity sub = subjectRepository.save(new SubjectEntity(UUID.randomUUID(), "S2_" + UUID.randomUUID().toString().substring(0, 6), "Networks", "THEORY", 3, dept.getId()));
        StudentEntity student = studentRepository.save(new StudentEntity(UUID.randomUUID(), "REG_AR2_" + UUID.randomUUID().toString().substring(0, 6), "Charlie", "charlie_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));

        SessionEntity session = sessionRepository.save(new SessionEntity(
            UUID.randomUUID(), sub.getId(), sec.getId(), fac.getId(), period.getId(),
            LocalDate.now(), "THEORY", 1, 1, "CONDUCTED", null, null));

        // First insertion succeeds
        AttendanceRecordEntity r1 = new AttendanceRecordEntity(UUID.randomUUID(), session.getId(), student.getId(), "PRESENT");
        recordRepository.saveAndFlush(r1);

        // Second insertion for same (session_id, student_id) must be rejected by PostgreSQL unique constraint
        AttendanceRecordEntity r2 = new AttendanceRecordEntity(UUID.randomUUID(), session.getId(), student.getId(), "ABSENT");

        assertThatThrownBy(() -> recordRepository.saveAndFlush(r2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
