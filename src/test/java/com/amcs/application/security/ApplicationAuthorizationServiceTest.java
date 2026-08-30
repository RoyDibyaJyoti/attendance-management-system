package com.amcs.application.security;

import com.amcs.application.exception.AccessDeniedException;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationAuthorizationServiceTest {

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private FacultyAssignmentRepositoryPort facultyAssignmentPort;

    private ApplicationAuthorizationService authorizationService;

    private final UUID studentAId = UUID.randomUUID();
    private final UUID studentBId = UUID.randomUUID();
    private final UUID facultyAId = UUID.randomUUID();
    private final UUID facultyBId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authorizationService = new ApplicationAuthorizationService(currentUserPort, facultyAssignmentPort);
    }

    private AuthenticatedActor studentActor(UUID studentId) {
        return new AuthenticatedActor(
            UUID.randomUUID(), "student", UserRole.STUDENT, Optional.of(studentId), Optional.empty()
        );
    }

    private AuthenticatedActor facultyActor(UUID facultyId) {
        return new AuthenticatedActor(
            UUID.randomUUID(), "faculty", UserRole.FACULTY, Optional.empty(), Optional.of(facultyId)
        );
    }

    private AuthenticatedActor adminActor() {
        return new AuthenticatedActor(
            adminId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty()
        );
    }

    private Session createSession(UUID conductingFacultyId) {
        return new Session(
            sessionId,
            subjectId,
            sectionId,
            conductingFacultyId,
            LocalDate.of(2026, 9, 1),
            SessionType.THEORY,
            1,
            0,
            SessionStatus.SCHEDULED,
            Optional.empty(),
            Optional.empty()
        );
    }

    @Nested
    @DisplayName("Student IDOR Protection Tests")
    class StudentIdorTests {

        @Test
        @DisplayName("1. Student can access own attendance")
        void shouldAllowStudentToAccessOwnAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(studentActor(studentAId));

            assertThatCode(() -> authorizationService.requireStudentSelfAccess(studentAId))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("2. Student cannot access another student's attendance (403 ACCESS_DENIED)")
        void shouldDenyStudentAccessToOtherStudentAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(studentActor(studentAId));

            assertThatThrownBy(() -> authorizationService.requireStudentSelfAccess(studentBId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("3 & 4. Student cannot access another student's subject shortage or projection")
        void shouldDenyStudentAccessToOtherStudentSubjectAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(studentActor(studentAId));

            assertThatThrownBy(() -> authorizationService.requireSubjectAttendanceReadAccess(
                studentBId, subjectId, sectionId, periodId
            ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("5. HOD_ADMIN can access another student's attendance")
        void shouldAllowAdminToAccessAnyStudentAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(adminActor());

            assertThatCode(() -> authorizationService.requireStudentSelfAccess(studentBId))
                .doesNotThrowAnyException();

            assertThatCode(() -> authorizationService.requireSubjectAttendanceReadAccess(
                studentBId, subjectId, sectionId, periodId
            ))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Faculty Assignment Scope Tests")
    class FacultyAssignmentTests {

        @Test
        @DisplayName("6. Assigned faculty can perform authorized teaching operation (subject read)")
        void shouldAllowAssignedFacultyToReadSubjectAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyAId));
            when(facultyAssignmentPort.isFacultyAssigned(facultyAId, subjectId, sectionId, periodId))
                .thenReturn(true);

            assertThatCode(() -> authorizationService.requireSubjectAttendanceReadAccess(
                studentAId, subjectId, sectionId, periodId
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("7. Unassigned faculty cannot perform that operation (403 ACCESS_DENIED)")
        void shouldDenyUnassignedFacultyFromReadingSubjectAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyAId));
            when(facultyAssignmentPort.isFacultyAssigned(facultyAId, subjectId, sectionId, periodId))
                .thenReturn(false);

            assertThatThrownBy(() -> authorizationService.requireSubjectAttendanceReadAccess(
                studentAId, subjectId, sectionId, periodId
            ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Faculty member is not assigned");
        }

        @Test
        @DisplayName("8. Faculty cannot impersonate another faculty member through a request field")
        void shouldPreventFacultyImpersonationInSessionCreation() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyAId));

            // Faculty A attempts to create a session under Faculty B's ID
            assertThatThrownBy(() -> authorizationService.requireSessionCreationAccess(
                subjectId, sectionId, periodId, facultyBId
            ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Faculty cannot create sessions on behalf of another");
        }

        @Test
        @DisplayName("9. HOD_ADMIN bypasses assignment scope")
        void shouldAllowAdminToCreateSessionWithoutDirectAssignment() {
            when(currentUserPort.requireCurrentActor()).thenReturn(adminActor());

            assertThatCode(() -> authorizationService.requireSessionCreationAccess(
                subjectId, sectionId, periodId, facultyAId
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Session Conductor Authorization Tests")
    class SessionConductorTests {

        @Test
        @DisplayName("10. Correct conductor can perform conductor-restricted modification")
        void shouldAllowConductorToModifySession() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyAId));
            Session session = createSession(facultyAId);

            assertThatCode(() -> authorizationService.requireSessionModificationAccess(session))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("11. Different unassigned faculty member cannot modify session")
        void shouldDenyDifferentUnassignedFacultyFromModifyingSession() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyBId));
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of());
            Session session = createSession(facultyAId);

            assertThatThrownBy(() -> authorizationService.requireSessionModificationAccess(session))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only assigned or conducting faculty");
        }

        @Test
        @DisplayName("12. HOD_ADMIN can modify session regardless of conductor")
        void shouldAllowAdminToModifySession() {
            when(currentUserPort.requireCurrentActor()).thenReturn(adminActor());
            Session session = createSession(facultyAId);

            assertThatCode(() -> authorizationService.requireSessionModificationAccess(session))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Attendance Recording & Correction Tests")
    class AttendanceAuthorizationTests {

        @Test
        @DisplayName("13. Student cannot record attendance")
        void shouldDenyStudentFromRecordingAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(studentActor(studentAId));
            Session session = createSession(facultyAId);

            assertThatThrownBy(() -> authorizationService.requireAttendanceRecordingAccess(session))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Students are not permitted to record attendance");
        }

        @Test
        @DisplayName("14. Assigned faculty can record attendance")
        void shouldAllowAssignedFacultyToRecordAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyBId));
            Session session = createSession(facultyAId);

            FacultyAssignment assignment = new FacultyAssignment(
                UUID.randomUUID(), facultyBId, subjectId, sectionId, periodId, true, Instant.now()
            );
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of(assignment));

            assertThatCode(() -> authorizationService.requireAttendanceRecordingAccess(session))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("15. Unassigned faculty cannot record attendance")
        void shouldDenyUnassignedFacultyFromRecordingAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyBId));
            Session session = createSession(facultyAId);
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of());

            assertThatThrownBy(() -> authorizationService.requireAttendanceRecordingAccess(session))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Faculty is not assigned or authorized");
        }

        @Test
        @DisplayName("16. Student cannot correct attendance")
        void shouldDenyStudentFromCorrectingAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(studentActor(studentAId));
            Session session = createSession(facultyAId);

            assertThatThrownBy(() -> authorizationService.requireAttendanceCorrectionAccess(session))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Students are not permitted to correct attendance");
        }

        @Test
        @DisplayName("17. Unauthorized faculty cannot correct attendance")
        void shouldDenyUnauthorizedFacultyFromCorrectingAttendance() {
            when(currentUserPort.requireCurrentActor()).thenReturn(facultyActor(facultyBId));
            Session session = createSession(facultyAId);
            when(facultyAssignmentPort.findByFacultyId(facultyBId)).thenReturn(List.of());

            assertThatThrownBy(() -> authorizationService.requireAttendanceCorrectionAccess(session))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Faculty is not authorized to correct attendance");
        }

        @Test
        @DisplayName("18 & 19. HOD_ADMIN can perform administrative attendance recording & correction")
        void shouldAllowAdminToPerformAttendanceOperations() {
            when(currentUserPort.requireCurrentActor()).thenReturn(adminActor());
            Session session = createSession(facultyAId);

            assertThatCode(() -> authorizationService.requireAttendanceRecordingAccess(session))
                .doesNotThrowAnyException();

            assertThatCode(() -> authorizationService.requireAttendanceCorrectionAccess(session))
                .doesNotThrowAnyException();
        }
    }
}
