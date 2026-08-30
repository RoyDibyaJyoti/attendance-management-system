package com.amcs.application.security;

import com.amcs.application.exception.AccessDeniedException;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.domain.attendance.Session;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure application-layer authorization service.
 * Enforces contextual business rules (IDOR prevention, faculty teaching scope,
 * session conductor boundaries, and administrative access).
 * Completely framework-independent; contains zero Spring Security, JWT, or JPA imports.
 */
@Component
public class ApplicationAuthorizationService {

    private final CurrentUserPort currentUserPort;
    private final FacultyAssignmentRepositoryPort facultyAssignmentPort;

    public ApplicationAuthorizationService(
        CurrentUserPort currentUserPort,
        FacultyAssignmentRepositoryPort facultyAssignmentPort
    ) {
        this.currentUserPort = Objects.requireNonNull(currentUserPort, "currentUserPort");
        this.facultyAssignmentPort = Objects.requireNonNull(facultyAssignmentPort, "facultyAssignmentPort");
    }

    /**
     * Enforces IDOR protection for student self-service endpoints (overview/overall summaries).
     */
    public void requireStudentSelfAccess(UUID targetStudentId) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return; // HOD_ADMIN can access any student's overview
        }
        if (actor.isStudent()) {
            UUID studentId = actor.studentId()
                .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
            if (!studentId.equals(targetStudentId)) {
                throw new AccessDeniedException("Access denied: You cannot view or access another student's attendance");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Faculty members cannot access student overall summaries");
    }

    /**
     * Enforces subject-level attendance access:
     * - Student: own attendance only (IDOR prevention)
     * - Faculty: must be assigned to the subject & section in the given academic period
     * - HOD_ADMIN: unrestricted
     */
    public void requireSubjectAttendanceReadAccess(
        UUID targetStudentId, UUID subjectId, UUID sectionId, UUID academicPeriodId
    ) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            UUID studentId = actor.studentId()
                .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
            if (!studentId.equals(targetStudentId)) {
                throw new AccessDeniedException("Access denied: You cannot view or access another student's attendance");
            }
            return;
        }
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId()
                .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));
            boolean assigned = facultyAssignmentPort.isFacultyAssigned(facultyId, subjectId, sectionId, academicPeriodId);
            if (!assigned) {
                throw new AccessDeniedException("Access denied: Faculty member is not assigned to this subject and section");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to view subject attendance");
    }

    /**
     * Enforces teaching scope for session creation and prevents faculty identity impersonation.
     */
    public void requireSessionCreationAccess(
        UUID subjectId, UUID sectionId, UUID academicPeriodId, UUID requestedConductedByFacultyId
    ) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            throw new AccessDeniedException("Access denied: Students are not permitted to create sessions");
        }
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId()
                .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));

            if (requestedConductedByFacultyId != null && !requestedConductedByFacultyId.equals(facultyId)) {
                throw new AccessDeniedException("Access denied: Faculty cannot create sessions on behalf of another faculty member");
            }

            boolean assigned = facultyAssignmentPort.isFacultyAssigned(facultyId, subjectId, sectionId, academicPeriodId);
            if (!assigned) {
                throw new AccessDeniedException("Access denied: Faculty is not assigned to teach this subject and section");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to create sessions");
    }

    /**
     * Enforces conductor/assigned scope for modifying/cancelling/rescheduling a session.
     */
    public void requireSessionModificationAccess(Session session) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            throw new AccessDeniedException("Access denied: Students are not permitted to modify sessions");
        }
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId()
                .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));

            boolean isConductor = session.conductedByFacultyId().equals(facultyId);
            boolean isAssigned = facultyAssignmentPort.findByFacultyId(facultyId).stream()
                .anyMatch(a -> a.subjectId().equals(session.subjectId()) && a.sectionId().equals(session.sectionId()));

            if (!isConductor && !isAssigned) {
                throw new AccessDeniedException("Access denied: Only assigned or conducting faculty may modify this session");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to modify session");
    }

    /**
     * Enforces authorization to conduct roll-call and record batch attendance.
     */
    public void requireAttendanceRecordingAccess(Session session) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            throw new AccessDeniedException("Access denied: Students are not permitted to record attendance");
        }
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId()
                .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));

            boolean isConductor = session.conductedByFacultyId().equals(facultyId);
            boolean isAssigned = facultyAssignmentPort.findByFacultyId(facultyId).stream()
                .anyMatch(a -> a.subjectId().equals(session.subjectId()) && a.sectionId().equals(session.sectionId()));

            if (!isConductor && !isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty is not assigned or authorized to record attendance for this session");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to record attendance");
    }

    /**
     * Enforces authorization to submit an attendance correction.
     */
    public void requireAttendanceCorrectionAccess(Session session) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            throw new AccessDeniedException("Access denied: Students are not permitted to correct attendance records");
        }
        if (actor.isFaculty()) {
            UUID facultyId = actor.facultyId()
                .orElseThrow(() -> new AccessDeniedException("No faculty record linked to account"));

            boolean isConductor = session.conductedByFacultyId().equals(facultyId);
            boolean isAssigned = facultyAssignmentPort.findByFacultyId(facultyId).stream()
                .anyMatch(a -> a.subjectId().equals(session.subjectId()) && a.sectionId().equals(session.sectionId()));

            if (!isConductor && !isAssigned) {
                throw new AccessDeniedException("Access denied: Faculty is not authorized to correct attendance for this session");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to correct attendance");
    }

    /**
     * Enforces student profile read access.
     */
    public void requireStudentProfileReadAccess(UUID targetStudentId) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin() || actor.isFaculty()) {
            return;
        }
        if (actor.isStudent()) {
            UUID studentId = actor.studentId()
                .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
            if (!studentId.equals(targetStudentId)) {
                throw new AccessDeniedException("Access denied: Students may only view their own profile");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges");
    }

    /**
     * Enforces student profile update access (students self-update, HOD updates any).
     */
    public void requireStudentProfileUpdateAccess(UUID targetStudentId) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isStudent()) {
            UUID studentId = actor.studentId()
                .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
            if (!studentId.equals(targetStudentId)) {
                throw new AccessDeniedException("Access denied: Students may only update their own profile");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Faculty members cannot update student profiles");
    }

    /**
     * Enforces administrative-only access.
     */
    public void requireAdminOnly(String operation) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (!actor.isAdmin()) {
            throw new AccessDeniedException("Access denied: Only administrators may " + operation);
        }
    }

    /**
     * Enforces that the actor is either FACULTY or HOD_ADMIN (students forbidden).
     */
    public void requireFacultyOrAdmin(String operation) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin() || actor.isFaculty()) {
            return;
        }
        throw new AccessDeniedException("Access denied: Students are not permitted to " + operation);
    }

    /**
     * Enforces student enrollment history read access:
     * - Student: own enrollment history only
     * - Faculty: permitted
     * - HOD_ADMIN: permitted
     */
    public void requireStudentEnrollmentReadAccess(UUID targetStudentId) {
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();
        if (actor.isAdmin() || actor.isFaculty()) {
            return;
        }
        if (actor.isStudent()) {
            UUID studentId = actor.studentId()
                .orElseThrow(() -> new AccessDeniedException("No student record linked to account"));
            if (!studentId.equals(targetStudentId)) {
                throw new AccessDeniedException("Access denied: Students may only view their own enrollment history");
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient privileges to view enrollment history");
    }
}
