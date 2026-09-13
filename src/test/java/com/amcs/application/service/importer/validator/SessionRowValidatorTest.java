package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.importer.payload.StagedSessionPayload;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.academic.SubjectComponent;
import com.amcs.domain.attendance.SessionType;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionRowValidator Unit Tests")
class SessionRowValidatorTest {

    @Mock private SubjectRepositoryPort subjectRepository;
    @Mock private SectionRepositoryPort sectionRepository;
    @Mock private FacultyRepositoryPort facultyRepository;
    @Mock private LabGroupRepositoryPort labGroupRepository;
    @Mock private AcademicPeriodRepositoryPort academicPeriodRepository;
    @Mock private SessionRepositoryPort sessionRepository;
    @Mock private FacultyAssignmentRepositoryPort facultyAssignmentRepository;

    private SessionRowValidator validator;

    private final UUID periodId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Set<String> seenSessions;

    private final AuthenticatedActor adminActor = new AuthenticatedActor(
        userId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty()
    );

    @BeforeEach
    void setUp() {
        validator = new SessionRowValidator(
            subjectRepository, sectionRepository, facultyRepository, labGroupRepository,
            academicPeriodRepository, sessionRepository, facultyAssignmentRepository
        );
        seenSessions = new HashSet<>();
    }

    private ParsedRow createRow(String sub, String sec, String fac, String date, String type, String units) {
        Map<String, String> map = Map.of(
            "subject code", sub,
            "section name", sec,
            "faculty employee id", fac,
            "session date", date,
            "session type", type,
            "planned units", units
        );
        return new ParsedRow(2, map, List.of(sub, sec, fac, date, type, units), false);
    }

    @Test
    @DisplayName("Valid session row passes validation")
    void shouldValidateCleanSessionRow() {
        ParsedRow row = createRow("CS101", "A", "FAC001", "2026-09-15", "THEORY", "1");

        Subject subject = new Subject(subjectId, "Computer Science", "CS101", com.amcs.domain.academic.CourseType.THEORY, 3, true);
        SectionEntity section = new SectionEntity(sectionId, "A", UUID.randomUUID(), periodId);
        AcademicPeriod period = new AcademicPeriod("Fall 2026", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        FacultyEntity faculty = new FacultyEntity(facultyId, "FAC001", "Dr. Smith", "smith@univ.edu", UUID.randomUUID());

        when(subjectRepository.findByCode("CS101")).thenReturn(Optional.of(subject));
        when(sectionRepository.findAll()).thenReturn(List.of(section));
        when(academicPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(facultyRepository.findByEmployeeId("FAC001")).thenReturn(Optional.of(faculty));
        when(sessionRepository.findBySection(sectionId)).thenReturn(List.of());

        ValidationOutcome<StagedSessionPayload> outcome = validator.validateRow(row, seenSessions, adminActor);

        assertThat(outcome.isValid()).isTrue();
        StagedSessionPayload payload = outcome.getPayload().orElseThrow();
        assertThat(payload.sessionDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(payload.sessionType()).isEqualTo(SessionType.THEORY);
        assertThat(payload.plannedUnits()).isEqualTo(1);
    }

    @Test
    @DisplayName("Date outside academic period is rejected")
    void shouldRejectDateOutsidePeriod() {
        ParsedRow row = createRow("CS101", "A", "FAC001", "2026-08-01", "THEORY", "1"); // Before Sept 1

        Subject subject = new Subject(subjectId, "Computer Science", "CS101", com.amcs.domain.academic.CourseType.THEORY, 3, true);
        SectionEntity section = new SectionEntity(sectionId, "A", UUID.randomUUID(), periodId);
        AcademicPeriod period = new AcademicPeriod("Fall 2026", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        FacultyEntity faculty = new FacultyEntity(facultyId, "FAC001", "Dr. Smith", "smith@univ.edu", UUID.randomUUID());

        when(subjectRepository.findByCode("CS101")).thenReturn(Optional.of(subject));
        when(sectionRepository.findAll()).thenReturn(List.of(section));
        when(academicPeriodRepository.findById(periodId)).thenReturn(Optional.of(period));
        when(facultyRepository.findByEmployeeId("FAC001")).thenReturn(Optional.of(faculty));

        ValidationOutcome<StagedSessionPayload> outcome = validator.validateRow(row, seenSessions, adminActor);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("DATE_OUTSIDE_ACADEMIC_PERIOD");
    }
}
