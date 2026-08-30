package com.amcs.infrastructure.persistence;

import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SessionEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSessionRepository;
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

@DisplayName("Session Persistence Integration Tests")
class SessionPersistenceIntegrationTest extends PostgresIntegrationTestBase {

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
    @DisplayName("SE-01: Create and retrieve conducted session with planned and conducted units")
    void createAndRetrieveConductedSession() {
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(UUID.randomUUID(), "MECH_" + UUID.randomUUID().toString().substring(0, 6), "Mechanical"));
        FacultyEntity fac = facultyRepository.save(new FacultyEntity(UUID.randomUUID(), "FAC_" + UUID.randomUUID().toString().substring(0, 6), "Dr. Smith", "fac_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));
        AcademicPeriodEntity period = periodRepository.save(new AcademicPeriodEntity(UUID.randomUUID(), "Semester_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(3)));
        SectionEntity sec = sectionRepository.save(new SectionEntity(UUID.randomUUID(), "A", dept.getId(), period.getId()));
        SubjectEntity sub = subjectRepository.save(new SubjectEntity(UUID.randomUUID(), "SUB_" + UUID.randomUUID().toString().substring(0, 6), "Thermodynamics", "THEORY", 4, dept.getId()));

        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity(
            sessionId, sub.getId(), sec.getId(), fac.getId(), period.getId(),
            LocalDate.now(), "THEORY", 1, 1, "CONDUCTED", null, null);

        sessionRepository.save(session);

        var found = sessionRepository.findById(sessionId);
        assertThat(found).isPresent();
        assertThat(found.get().getPlannedUnits()).isEqualTo(1);
        assertThat(found.get().getConductedUnits()).isEqualTo(1);
        assertThat(found.get().getStatus()).isEqualTo("CONDUCTED");
    }

    @Test
    @Transactional
    @DisplayName("SE-02: Cancelled session must have conducted_units = 0")
    void cancelledSession_persistedWithZeroConductedUnits() {
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(UUID.randomUUID(), "CIVIL_" + UUID.randomUUID().toString().substring(0, 6), "Civil"));
        FacultyEntity fac = facultyRepository.save(new FacultyEntity(UUID.randomUUID(), "FAC_" + UUID.randomUUID().toString().substring(0, 6), "Dr. Jones", "fac_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));
        AcademicPeriodEntity period = periodRepository.save(new AcademicPeriodEntity(UUID.randomUUID(), "Semester_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(3)));
        SectionEntity sec = sectionRepository.save(new SectionEntity(UUID.randomUUID(), "B", dept.getId(), period.getId()));
        SubjectEntity sub = subjectRepository.save(new SubjectEntity(UUID.randomUUID(), "SUB_" + UUID.randomUUID().toString().substring(0, 6), "Structures", "THEORY", 3, dept.getId()));

        UUID sessionId = UUID.randomUUID();
        SessionEntity cancelledSession = new SessionEntity(
            sessionId, sub.getId(), sec.getId(), fac.getId(), period.getId(),
            LocalDate.now(), "THEORY", 3, 0, "CANCELLED", null, null);

        sessionRepository.save(cancelledSession);

        var found = sessionRepository.findById(sessionId);
        assertThat(found).isPresent();
        assertThat(found.get().getPlannedUnits()).isEqualTo(3);
        assertThat(found.get().getConductedUnits()).isZero();
        assertThat(found.get().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("SE-03: Database check constraint chk_session_conducted_units rejects CANCELLED with conducted_units > 0")
    void invalidSessionConductedUnits_rejectedByDatabaseCheckConstraint() {
        DepartmentEntity dept = departmentRepository.save(new DepartmentEntity(UUID.randomUUID(), "DEPT_" + UUID.randomUUID().toString().substring(0, 6), "Dept"));
        FacultyEntity fac = facultyRepository.save(new FacultyEntity(UUID.randomUUID(), "FAC_" + UUID.randomUUID().toString().substring(0, 6), "Prof", "fac_" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu", dept.getId()));
        AcademicPeriodEntity period = periodRepository.save(new AcademicPeriodEntity(UUID.randomUUID(), "Period_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(3)));
        SectionEntity sec = sectionRepository.save(new SectionEntity(UUID.randomUUID(), "C", dept.getId(), period.getId()));
        SubjectEntity sub = subjectRepository.save(new SubjectEntity(UUID.randomUUID(), "SUB_" + UUID.randomUUID().toString().substring(0, 6), "Subject", "LABORATORY", 2, dept.getId()));

        // Invalid: CANCELLED but conducted_units = 3
        SessionEntity invalidSession = new SessionEntity(
            UUID.randomUUID(), sub.getId(), sec.getId(), fac.getId(), period.getId(),
            LocalDate.now(), "LAB", 3, 3, "CANCELLED", null, null);

        assertThatThrownBy(() -> sessionRepository.saveAndFlush(invalidSession))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
