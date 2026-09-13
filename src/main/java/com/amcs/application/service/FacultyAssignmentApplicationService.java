package com.amcs.application.service;

import com.amcs.application.dto.assignment.CreateFacultyAssignmentRequest;
import com.amcs.application.dto.assignment.FacultyAssignmentResponse;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FacultyAssignmentApplicationService {

    private final FacultyAssignmentRepositoryPort assignmentPort;
    private final FacultyRepositoryPort facultyPort;
    private final SubjectRepositoryPort subjectPort;
    private final SectionRepositoryPort sectionPort;
    private final AcademicPeriodRepositoryPort periodPort;
    private final ApplicationAuthorizationService authorizationService;

    public FacultyAssignmentApplicationService(
        FacultyAssignmentRepositoryPort assignmentPort,
        FacultyRepositoryPort facultyPort,
        SubjectRepositoryPort subjectPort,
        SectionRepositoryPort sectionPort,
        AcademicPeriodRepositoryPort periodPort,
        ApplicationAuthorizationService authorizationService
    ) {
        this.assignmentPort = Objects.requireNonNull(assignmentPort, "assignmentPort");
        this.facultyPort = Objects.requireNonNull(facultyPort, "facultyPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public FacultyAssignmentResponse createAssignment(CreateFacultyAssignmentRequest request) {
        authorizationService.requireAdminOnly("assign faculty to subjects");

        // Validate dependencies
        facultyPort.findById(request.facultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));
        subjectPort.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        sectionPort.findById(request.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        periodPort.findById(request.academicPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found"));

        if (request.assignmentEnd() != null && request.assignmentEnd().isBefore(request.assignmentStart())) {
            throw new InvalidBusinessOperationException("Assignment end date cannot be before start date");
        }

        // Check for temporal overlaps for this teaching context
        List<FacultyAssignment> overlaps = assignmentPort.findOverlappingAssignments(
            request.subjectId(),
            request.sectionId(),
            request.academicPeriodId(),
            request.assignmentStart(),
            request.assignmentEnd()
        );

        if (!overlaps.isEmpty()) {
            throw new InvalidBusinessOperationException("Overlapping faculty assignment found for this subject and section during the specified dates");
        }

        FacultyAssignment assignment = new FacultyAssignment(
            UUID.randomUUID(),
            request.facultyId(),
            request.subjectId(),
            request.sectionId(),
            request.academicPeriodId(),
            request.assignmentStart(),
            request.assignmentEnd(),
            "ACTIVE",
            java.time.Instant.now()
        );

        FacultyAssignment saved = assignmentPort.save(assignment);
        return toResponse(saved);
    }

    public List<FacultyAssignmentResponse> listAssignmentsByFaculty(UUID facultyId) {
        authorizationService.requireFacultyProfileReadAccess(facultyId);
        return assignmentPort.findByFacultyId(facultyId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FacultyAssignmentResponse> listAssignmentsByFacultyAndPeriod(UUID facultyId, UUID periodId) {
        authorizationService.requireFacultyProfileReadAccess(facultyId);
        return assignmentPort.findByFacultyIdAndAcademicPeriodId(facultyId, periodId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FacultyAssignmentResponse> listAssignmentsBySection(UUID sectionId) {
        authorizationService.requireAdminOnly("list all assignments for a section");
        return assignmentPort.findBySectionId(sectionId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public FacultyAssignmentResponse endAssignment(UUID assignmentId, LocalDate endDate) {
        authorizationService.requireAdminOnly("end faculty assignment");
        FacultyAssignment existing = assignmentPort.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
            
        if (existing.assignmentEnd() != null && existing.assignmentEnd().isBefore(endDate)) {
            throw new InvalidBusinessOperationException("Assignment already ended before this date");
        }
        if (existing.assignmentStart().isAfter(endDate)) {
            throw new InvalidBusinessOperationException("End date cannot be before start date");
        }
        
        FacultyAssignment updated = new FacultyAssignment(
            existing.id(),
            existing.facultyId(),
            existing.subjectId(),
            existing.sectionId(),
            existing.academicPeriodId(),
            existing.assignmentStart(),
            endDate,
            existing.status(),
            existing.createdAt()
        );
        
        FacultyAssignment saved = assignmentPort.save(updated);
        return toResponse(saved);
    }

    private FacultyAssignmentResponse toResponse(FacultyAssignment assignment) {
        return new FacultyAssignmentResponse(
            assignment.id(),
            assignment.facultyId(),
            assignment.subjectId(),
            assignment.sectionId(),
            assignment.academicPeriodId(),
            assignment.assignmentStart(),
            assignment.assignmentEnd(),
            assignment.status(),
            assignment.createdAt()
        );
    }
}
