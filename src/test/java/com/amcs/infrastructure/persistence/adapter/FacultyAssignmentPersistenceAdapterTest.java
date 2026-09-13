package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.FacultyAssignmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.mapper.FacultyAssignmentPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyAssignmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultyAssignmentPersistenceAdapterTest {

    @Mock private SpringDataFacultyAssignmentRepository assignmentRepository;
    @Mock private SpringDataFacultyRepository facultyRepository;
    @Mock private SpringDataSubjectRepository subjectRepository;
    @Mock private SpringDataSectionRepository sectionRepository;
    @Mock private SpringDataAcademicPeriodRepository periodRepository;

    private FacultyAssignmentPersistenceMapper mapper;
    private FacultyAssignmentPersistenceAdapter adapter;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mapper = new FacultyAssignmentPersistenceMapper();
        adapter = new FacultyAssignmentPersistenceAdapter(
            assignmentRepository, mapper, facultyRepository, subjectRepository, sectionRepository, periodRepository);
    }

    @Test
    @DisplayName("Should save faculty teaching assignment")
    void shouldSaveAssignment() {
        FacultyAssignment assignment = new FacultyAssignment(
            assignmentId, facultyId, subjectId, sectionId, periodId, LocalDate.now(), null, "ACTIVE", Instant.now());

        FacultyEntity faculty = new FacultyEntity(facultyId, "EMP01", "Prof Jones", "jones@univ.edu", UUID.randomUUID());
        SubjectEntity subject = new SubjectEntity(subjectId, "CS101", "Intro CS", "THEORY", 3, UUID.randomUUID());
        SectionEntity section = new SectionEntity(sectionId, "Sec A", UUID.randomUUID(), periodId);
        AcademicPeriodEntity period = new AcademicPeriodEntity(periodId, "Fall 2026", LocalDate.now(), LocalDate.now().plusMonths(4));

        when(facultyRepository.findById(facultyId)).thenReturn(Optional.of(faculty));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(assignmentRepository.save(any(FacultyAssignmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FacultyAssignment saved = adapter.save(assignment);

        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo(assignmentId);
        assertThat(saved.facultyId()).isEqualTo(facultyId);
        assertThat(saved.subjectId()).isEqualTo(subjectId);
        assertThat(saved.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should verify faculty assignment scope via isFacultyAssigned")
    void shouldVerifyFacultyAssignmentScope() {
        when(assignmentRepository.isFacultyAssigned(facultyId, subjectId, sectionId, periodId, LocalDate.now()))
            .thenReturn(true);

        boolean assigned = adapter.isFacultyAssigned(facultyId, subjectId, sectionId, periodId, LocalDate.now());
        assertThat(assigned).isTrue();

        when(assignmentRepository.isFacultyAssigned(facultyId, subjectId, sectionId, periodId, LocalDate.now()))
            .thenReturn(false);

        boolean notAssigned = adapter.isFacultyAssigned(facultyId, subjectId, sectionId, periodId, LocalDate.now());
        assertThat(notAssigned).isFalse();
    }

    @Test
    @DisplayName("Should find assignments by faculty ID")
    void shouldFindByFacultyId() {
        FacultyEntity faculty = new FacultyEntity(facultyId, "EMP01", "Prof Jones", "jones@univ.edu", UUID.randomUUID());
        SubjectEntity subject = new SubjectEntity(subjectId, "CS101", "Intro CS", "THEORY", 3, UUID.randomUUID());
        SectionEntity section = new SectionEntity(sectionId, "Sec A", UUID.randomUUID(), periodId);
        AcademicPeriodEntity period = new AcademicPeriodEntity(periodId, "Fall 2026", LocalDate.now(), LocalDate.now().plusMonths(4));

        FacultyAssignmentEntity entity = new FacultyAssignmentEntity(
            assignmentId, faculty, subject, section, period, LocalDate.now(), null, "ACTIVE");

        when(assignmentRepository.findByFacultyId(facultyId)).thenReturn(List.of(entity));

        List<FacultyAssignment> results = adapter.findByFacultyId(facultyId);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().facultyId()).isEqualTo(facultyId);
    }
}
