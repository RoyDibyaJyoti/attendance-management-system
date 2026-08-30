package com.amcs.application.service;

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
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionApplicationServiceTest {

    @Mock private SessionRepositoryPort sessionPort;
    @Mock private SubjectRepositoryPort subjectPort;
    @Mock private SectionRepositoryPort sectionPort;
    @Mock private FacultyRepositoryPort facultyPort;
    @Mock private AcademicPeriodRepositoryPort periodPort;
    @Mock private com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    private SessionApplicationService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        service = new SessionApplicationService(sessionPort, subjectPort, sectionPort, facultyPort, periodPort, authorizationService);
    }

    @Test
    @DisplayName("Create session sets status SCHEDULED with 0 conducted units")
    void shouldCreateScheduledSession() {
        CreateSessionRequest request = new CreateSessionRequest(
            subjectId, sectionId, facultyId, periodId, today, "THEORY", 2, null);

        when(subjectPort.findById(subjectId)).thenReturn(Optional.of(new Subject(subjectId, "Math", "MATH101", CourseType.THEORY, 4)));
        when(sectionPort.findById(sectionId)).thenReturn(Optional.of(new SectionEntity(sectionId, "Sec A", UUID.randomUUID(), periodId)));
        when(facultyPort.findById(facultyId)).thenReturn(Optional.of(new FacultyEntity(facultyId, "FAC01", "Dr Smith", "smith@univ.edu", UUID.randomUUID())));
        when(periodPort.findById(periodId)).thenReturn(Optional.of(new AcademicPeriod("Fall 2026", today, today.plusMonths(4))));
        when(sessionPort.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.createSession(request);

        assertThat(response.status()).isEqualTo("SCHEDULED");
        assertThat(response.plannedUnits()).isEqualTo(2);
        assertThat(response.conductedUnits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cancel scheduled session transitions to CANCELLED with conductedUnits = 0")
    void shouldCancelScheduledSession() {
        Session scheduled = new Session(
            sessionId, subjectId, sectionId, facultyId, today, SessionType.THEORY, 2, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());

        when(sessionPort.findById(sessionId)).thenReturn(Optional.of(scheduled));
        when(sessionPort.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.cancelSession(sessionId, new CancelSessionRequest("Faculty on sick leave"));

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.conductedUnits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cancelling an already CONDUCTED session throws InvalidSessionTransitionException")
    void shouldProhibitCancellingConductedSession() {
        Session conducted = new Session(
            sessionId, subjectId, sectionId, facultyId, today, SessionType.THEORY, 2, 2, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());

        when(sessionPort.findById(sessionId)).thenReturn(Optional.of(conducted));

        assertThatThrownBy(() -> service.cancelSession(sessionId, new CancelSessionRequest("Reason")))
            .isInstanceOf(InvalidSessionTransitionException.class)
            .hasMessageContaining("Cannot cancel an already CONDUCTED session");
    }

    @Test
    @DisplayName("Reschedule creates replacement session and sets RESCHEDULED status")
    void shouldRescheduleSession() {
        Session scheduled = new Session(
            sessionId, subjectId, sectionId, facultyId, today, SessionType.THEORY, 2, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());

        when(sessionPort.findById(sessionId)).thenReturn(Optional.of(scheduled));
        when(sessionPort.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDate = today.plusDays(3);
        SessionResponse response = service.rescheduleSession(sessionId, new RescheduleSessionRequest(newDate, "Lab maintenance"));

        assertThat(response.status()).isEqualTo("RESCHEDULED");
        assertThat(response.replacedBySessionId()).isNotNull();
    }
}
