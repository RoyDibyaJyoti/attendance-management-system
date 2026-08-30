package com.amcs.application.service;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.session.CancelSessionRequest;
import com.amcs.application.dto.session.CreateSessionRequest;
import com.amcs.application.dto.session.RescheduleSessionRequest;
import com.amcs.application.dto.session.SessionResponse;
import com.amcs.application.exception.InvalidSessionTransitionException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SessionApplicationService {

    private final SessionRepositoryPort sessionPort;
    private final SubjectRepositoryPort subjectPort;
    private final SectionRepositoryPort sectionPort;
    private final FacultyRepositoryPort facultyPort;
    private final AcademicPeriodRepositoryPort periodPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public SessionApplicationService(
        SessionRepositoryPort sessionPort,
        SubjectRepositoryPort subjectPort,
        SectionRepositoryPort sectionPort,
        FacultyRepositoryPort facultyPort,
        AcademicPeriodRepositoryPort periodPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.sessionPort = Objects.requireNonNull(sessionPort, "sessionPort");
        this.subjectPort = Objects.requireNonNull(subjectPort, "subjectPort");
        this.sectionPort = Objects.requireNonNull(sectionPort, "sectionPort");
        this.facultyPort = Objects.requireNonNull(facultyPort, "facultyPort");
        this.periodPort = Objects.requireNonNull(periodPort, "periodPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        if (authorizationService != null) {
            authorizationService.requireSessionCreationAccess(
                request.subjectId(), request.sectionId(), request.academicPeriodId(), request.conductedByFacultyId()
            );
        }
        subjectPort.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        sectionPort.findById(request.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.sectionId()));
        facultyPort.findById(request.conductedByFacultyId())
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + request.conductedByFacultyId()));
        periodPort.findById(request.academicPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("Academic period not found: " + request.academicPeriodId()));

        UUID sessionId = UUID.randomUUID();
        Session domain = new Session(
            sessionId,
            request.subjectId(),
            request.sectionId(),
            request.conductedByFacultyId(),
            request.sessionDate(),
            SessionType.valueOf(request.sessionType()),
            request.plannedUnits(),
            0, // Initial conducted units must be 0 for SCHEDULED
            SessionStatus.SCHEDULED,
            Optional.ofNullable(request.labGroupId()),
            Optional.empty()
        );

        Session saved = sessionPort.save(domain, request.academicPeriodId());
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse cancelSession(UUID sessionId, CancelSessionRequest request) {
        Session existing = sessionPort.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (authorizationService != null) {
            authorizationService.requireSessionModificationAccess(existing);
        }

        if (existing.status() == SessionStatus.CONDUCTED) {
            throw new InvalidSessionTransitionException(
                "Cannot cancel an already CONDUCTED session. Use administrative voiding workflow.");
        }
        if (existing.status() == SessionStatus.CANCELLED) {
            return toResponse(existing); // Idempotent
        }

        Session cancelled = new Session(
            existing.id(),
            existing.subjectId(),
            existing.sectionId(),
            existing.conductedByFacultyId(),
            existing.sessionDate(),
            existing.sessionType(),
            existing.plannedUnits(),
            0, // Cancelled session strictly has 0 conducted units
            SessionStatus.CANCELLED,
            existing.labGroupId(),
            existing.replacedBySessionId()
        );

        // Fetch academic period ID (from subject/section)
        Session saved = sessionPort.save(cancelled, UUID.randomUUID());
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse rescheduleSession(UUID sessionId, RescheduleSessionRequest request) {
        Session existing = sessionPort.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (authorizationService != null) {
            authorizationService.requireSessionModificationAccess(existing);
        }

        if (existing.status() == SessionStatus.CONDUCTED) {
            throw new InvalidSessionTransitionException(
                "Cannot reschedule an already CONDUCTED session.");
        }

        // Create replacement session
        UUID replacementId = UUID.randomUUID();
        Session replacement = new Session(
            replacementId,
            existing.subjectId(),
            existing.sectionId(),
            existing.conductedByFacultyId(),
            request.newSessionDate(),
            existing.sessionType(),
            existing.plannedUnits(),
            0,
            SessionStatus.SCHEDULED,
            existing.labGroupId(),
            Optional.empty()
        );
        sessionPort.save(replacement, UUID.randomUUID());

        // Mark original as RESCHEDULED linking replacement
        Session rescheduled = new Session(
            existing.id(),
            existing.subjectId(),
            existing.sectionId(),
            existing.conductedByFacultyId(),
            existing.sessionDate(),
            existing.sessionType(),
            existing.plannedUnits(),
            0,
            SessionStatus.RESCHEDULED,
            existing.labGroupId(),
            Optional.of(replacementId)
        );

        Session saved = sessionPort.save(rescheduled, UUID.randomUUID());
        return toResponse(saved);
    }

    public SessionResponse getSessionById(UUID id) {
        return sessionPort.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + id));
    }

    public PagedResponse<SessionResponse> listSessionsBySection(UUID sectionId, int page, int size) {
        List<SessionResponse> all = sessionPort.findBySection(sectionId).stream()
            .map(this::toResponse)
            .toList();
        return PagedResponse.of(all, page, size);
    }

    private SessionResponse toResponse(Session session) {
        return new SessionResponse(
            session.id(),
            session.subjectId(),
            session.sectionId(),
            session.conductedByFacultyId(),
            session.sessionDate(),
            session.sessionType().name(),
            session.plannedUnits(),
            session.conductedUnits(),
            session.status().name(),
            session.labGroupId().orElse(null),
            session.replacedBySessionId().orElse(null),
            0
        );
    }
}
