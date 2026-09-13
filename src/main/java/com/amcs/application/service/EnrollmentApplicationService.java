package com.amcs.application.service;

import com.amcs.application.dto.enrollment.EnrollStudentRequest;
import com.amcs.application.dto.enrollment.EnrollmentResponse;
import com.amcs.application.dto.enrollment.TransferStudentRequest;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.domain.enrollment.Enrollment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EnrollmentApplicationService {

    private final EnrollmentRepositoryPort enrollmentPort;
    private final StudentRepositoryPort studentPort;
    private final SectionRepositoryPort sectionPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public EnrollmentApplicationService(
        EnrollmentRepositoryPort enrollmentPort,
        StudentRepositoryPort studentPort,
        SectionRepositoryPort sectionPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.enrollmentPort = Objects.requireNonNull(enrollmentPort, "enrollmentPort");
        this.studentPort = Objects.requireNonNull(studentPort, "studentPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public EnrollmentResponse enrollStudent(EnrollStudentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("enroll students");
        }
        studentPort.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.studentId()));
        sectionPort.findById(request.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.sectionId()));

        if (request.enrollmentEnd() != null && request.enrollmentEnd().isBefore(request.enrollmentStart())) {
            throw new InvalidBusinessOperationException("Enrollment end date cannot be before start date");
        }

        Enrollment domain = new Enrollment(
            request.studentId(),
            request.sectionId(),
            request.enrollmentStart(),
            Optional.ofNullable(request.enrollmentEnd()),
            Optional.empty()
        );

        Enrollment saved = enrollmentPort.save(domain);
        return new EnrollmentResponse(
            UUID.randomUUID(),
            saved.studentId(),
            saved.sectionId(),
            saved.enrollmentStart(),
            saved.enrollmentEnd().orElse(null),
            "ACTIVE"
        );
    }

    @Transactional
    public EnrollmentResponse transferStudent(TransferStudentRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("transfer students");
        }
        studentPort.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.studentId()));
        sectionPort.findById(request.toSectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Target section not found: " + request.toSectionId()));

        Enrollment active = enrollmentPort.findActiveEnrollment(request.studentId(), request.fromSectionId())
            .orElseThrow(() -> new InvalidBusinessOperationException(
                "No active enrollment found in source section [%s] for student [%s]"
                    .formatted(request.fromSectionId(), request.studentId())));

        if (request.transferDate().isBefore(active.enrollmentStart())) {
            throw new InvalidBusinessOperationException("Transfer date cannot precede original enrollment start date");
        }

        // Close previous enrollment
        Enrollment closed = new Enrollment(
            active.studentId(),
            active.sectionId(),
            active.enrollmentStart(),
            Optional.of(request.transferDate().minusDays(1)),
            active.labGroupId()
        );
        enrollmentPort.save(closed);

        // Open new enrollment in target section
        Enrollment newEnrollment = new Enrollment(
            request.studentId(),
            request.toSectionId(),
            request.transferDate(),
            Optional.empty(),
            Optional.empty()
        );
        Enrollment saved = enrollmentPort.save(newEnrollment);

        return new EnrollmentResponse(
            UUID.randomUUID(),
            saved.studentId(),
            saved.sectionId(),
            saved.enrollmentStart(),
            saved.enrollmentEnd().orElse(null),
            "ACTIVE"
        );
    }

    public List<EnrollmentResponse> getStudentEnrollmentHistory(UUID studentId) {
        if (authorizationService != null) {
            authorizationService.requireStudentEnrollmentReadAccess(studentId);
        }
        return enrollmentPort.findByStudent(studentId).stream()
            .map(e -> new EnrollmentResponse(
                UUID.randomUUID(),
                e.studentId(),
                e.sectionId(),
                e.enrollmentStart(),
                e.enrollmentEnd().orElse(null),
                e.enrollmentEnd().isEmpty() ? "ACTIVE" : "ENDED"
            ))
            .toList();
    }

    public List<EnrollmentResponse> getSectionEnrollments(UUID sectionId) {
        if (authorizationService != null) {
            authorizationService.requireFacultyOrAdmin("view section enrollments");
        }
        return enrollmentPort.findBySection(sectionId).stream()
            .map(e -> new EnrollmentResponse(
                UUID.randomUUID(),
                e.studentId(),
                e.sectionId(),
                e.enrollmentStart(),
                e.enrollmentEnd().orElse(null),
                e.enrollmentEnd().isEmpty() ? "ACTIVE" : "ENDED"
            ))
            .toList();
    }
}
