package com.amcs.application.service;

import com.amcs.application.dto.assignment.CreateFacultyAssignmentRequest;
import com.amcs.application.dto.assignment.FacultyAssignmentResponse;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyAssignmentApplicationServiceTest {

    @Mock private FacultyAssignmentRepositoryPort assignmentPort;
    @Mock private FacultyRepositoryPort facultyPort;
    @Mock private SubjectRepositoryPort subjectPort;
    @Mock private SectionRepositoryPort sectionPort;
    @Mock private AcademicPeriodRepositoryPort periodPort;
    @Mock private ApplicationAuthorizationService authorizationService;

    @InjectMocks
    private FacultyAssignmentApplicationService assignmentService;

    private UUID facultyId;
    private UUID subjectId;
    private UUID sectionId;
    private UUID periodId;

    @BeforeEach
    void setUp() {
        facultyId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        sectionId = UUID.randomUUID();
        periodId = UUID.randomUUID();
    }

    @Test
    void testCreateAssignment_Success() {
        CreateFacultyAssignmentRequest req = new CreateFacultyAssignmentRequest(
            facultyId, subjectId, sectionId, periodId, LocalDate.of(2026, 8, 1), null
        );

        when(facultyPort.findById(facultyId)).thenReturn(Optional.of(mock(FacultyEntity.class)));
        when(subjectPort.findById(subjectId)).thenReturn(Optional.of(new Subject(subjectId, "S", "S", CourseType.THEORY, 3, true)));
        when(sectionPort.findById(sectionId)).thenReturn(Optional.of(mock(SectionEntity.class)));
        when(periodPort.findById(periodId)).thenReturn(Optional.of(new AcademicPeriod("P", LocalDate.now(), LocalDate.now().plusDays(1))));

        when(assignmentPort.findOverlappingAssignments(subjectId, sectionId, periodId, req.assignmentStart(), req.assignmentEnd()))
            .thenReturn(List.of());

        FacultyAssignment saved = new FacultyAssignment(
            UUID.randomUUID(), facultyId, subjectId, sectionId, periodId, req.assignmentStart(), null, "ACTIVE", Instant.now()
        );
        when(assignmentPort.save(any(FacultyAssignment.class))).thenReturn(saved);

        FacultyAssignmentResponse res = assignmentService.createAssignment(req);

        assertNotNull(res);
        assertEquals("ACTIVE", res.status());
        verify(authorizationService).requireAdminOnly("assign faculty to subjects");
    }

    @Test
    void testCreateAssignment_Overlap_ThrowsException() {
        CreateFacultyAssignmentRequest req = new CreateFacultyAssignmentRequest(
            facultyId, subjectId, sectionId, periodId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 1)
        );

        when(facultyPort.findById(facultyId)).thenReturn(Optional.of(mock(FacultyEntity.class)));
        when(subjectPort.findById(subjectId)).thenReturn(Optional.of(new Subject(subjectId, "S", "S", CourseType.THEORY, 3, true)));
        when(sectionPort.findById(sectionId)).thenReturn(Optional.of(mock(SectionEntity.class)));
        when(periodPort.findById(periodId)).thenReturn(Optional.of(new AcademicPeriod("P", LocalDate.now(), LocalDate.now().plusDays(1))));

        FacultyAssignment overlap = new FacultyAssignment(
            UUID.randomUUID(), facultyId, subjectId, sectionId, periodId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), "ACTIVE", Instant.now()
        );
        
        when(assignmentPort.findOverlappingAssignments(subjectId, sectionId, periodId, req.assignmentStart(), req.assignmentEnd()))
            .thenReturn(List.of(overlap));

        assertThrows(InvalidBusinessOperationException.class, () -> {
            assignmentService.createAssignment(req);
        });
    }
}
