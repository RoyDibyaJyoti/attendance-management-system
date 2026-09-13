package com.amcs.application.service;

import com.amcs.application.dto.enrollment.EnrollStudentRequest;
import com.amcs.application.dto.enrollment.EnrollmentResponse;
import com.amcs.application.dto.enrollment.TransferStudentRequest;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentApplicationServiceTest {

    @Mock
    private EnrollmentRepositoryPort enrollmentPort;

    @Mock
    private StudentRepositoryPort studentPort;

    @Mock
    private SectionRepositoryPort sectionPort;

    @Mock
    private ApplicationAuthorizationService authorizationService;

    private EnrollmentApplicationService service;

    private final UUID studentId = UUID.randomUUID();
    private final UUID sectionId1 = UUID.randomUUID();
    private final UUID sectionId2 = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EnrollmentApplicationService(
            enrollmentPort,
            studentPort,
            sectionPort,
            authorizationService
        );
    }

    @Test
    @DisplayName("Should successfully enroll a student into a section")
    void enrollStudent_Success() {
        when(studentPort.findById(studentId)).thenReturn(Optional.of(new StudentEntity(
            studentId, "STU001", "Alice", "alice@example.com", departmentId
        )));
        when(sectionPort.findById(sectionId1)).thenReturn(Optional.of(new SectionEntity(
            sectionId1, "CSE-A", departmentId, periodId
        )));

        LocalDate start = LocalDate.of(2026, 8, 1);
        EnrollStudentRequest request = new EnrollStudentRequest(studentId, sectionId1, start, null);

        when(enrollmentPort.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.enrollStudent(request);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.sectionId()).isEqualTo(sectionId1);
        assertThat(response.enrollmentStart()).isEqualTo(start);
        assertThat(response.status()).isEqualTo("ACTIVE");

        verify(authorizationService).requireAdminOnly("enroll students");
        verify(enrollmentPort).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should reject enrollment when student is not found")
    void enrollStudent_StudentNotFound() {
        when(studentPort.findById(studentId)).thenReturn(Optional.empty());

        EnrollStudentRequest request = new EnrollStudentRequest(studentId, sectionId1, LocalDate.of(2026, 8, 1), null);

        assertThatThrownBy(() -> service.enrollStudent(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Student not found");
    }

    @Test
    @DisplayName("Should reject enrollment when end date is before start date")
    void enrollStudent_InvalidDates() {
        when(studentPort.findById(studentId)).thenReturn(Optional.of(new StudentEntity(
            studentId, "STU001", "Alice", "alice@example.com", departmentId
        )));
        when(sectionPort.findById(sectionId1)).thenReturn(Optional.of(new SectionEntity(
            sectionId1, "CSE-A", departmentId, periodId
        )));

        EnrollStudentRequest request = new EnrollStudentRequest(
            studentId, sectionId1, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)
        );

        assertThatThrownBy(() -> service.enrollStudent(request))
            .isInstanceOf(InvalidBusinessOperationException.class)
            .hasMessageContaining("Enrollment end date cannot be before start date");
    }

    @Test
    @DisplayName("Should transfer student from source section to destination section, ending previous enrollment")
    void transferStudent_Success() {
        when(studentPort.findById(studentId)).thenReturn(Optional.of(new StudentEntity(
            studentId, "STU001", "Alice", "alice@example.com", departmentId
        )));
        when(sectionPort.findById(sectionId2)).thenReturn(Optional.of(new SectionEntity(
            sectionId2, "CSE-B", departmentId, periodId
        )));

        LocalDate originalStart = LocalDate.of(2026, 8, 1);
        Enrollment activeEnrollment = new Enrollment(
            studentId, sectionId1, originalStart, Optional.empty(), Optional.empty()
        );
        when(enrollmentPort.findActiveEnrollment(studentId, sectionId1)).thenReturn(Optional.of(activeEnrollment));

        LocalDate transferDate = LocalDate.of(2026, 9, 1);
        TransferStudentRequest request = new TransferStudentRequest(
            studentId, sectionId1, sectionId2, transferDate
        );

        when(enrollmentPort.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.transferStudent(request);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.sectionId()).isEqualTo(sectionId2);
        assertThat(response.enrollmentStart()).isEqualTo(transferDate);

        // Verify two save calls: 1 to close old enrollment, 1 to open new enrollment
        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentPort, times(2)).save(captor.capture());

        List<Enrollment> savedEnrollments = captor.getAllValues();
        Enrollment closedOld = savedEnrollments.get(0);
        assertThat(closedOld.sectionId()).isEqualTo(sectionId1);
        assertThat(closedOld.enrollmentEnd()).contains(LocalDate.of(2026, 8, 31));

        Enrollment openedNew = savedEnrollments.get(1);
        assertThat(openedNew.sectionId()).isEqualTo(sectionId2);
        assertThat(openedNew.enrollmentStart()).isEqualTo(transferDate);
        assertThat(openedNew.enrollmentEnd()).isEmpty();
    }

    @Test
    @DisplayName("Should reject transfer when transfer date precedes original enrollment start")
    void transferStudent_TransferDatePrecedesStart() {
        when(studentPort.findById(studentId)).thenReturn(Optional.of(new StudentEntity(
            studentId, "STU001", "Alice", "alice@example.com", departmentId
        )));
        when(sectionPort.findById(sectionId2)).thenReturn(Optional.of(new SectionEntity(
            sectionId2, "CSE-B", departmentId, periodId
        )));

        LocalDate originalStart = LocalDate.of(2026, 8, 15);
        Enrollment activeEnrollment = new Enrollment(
            studentId, sectionId1, originalStart, Optional.empty(), Optional.empty()
        );
        when(enrollmentPort.findActiveEnrollment(studentId, sectionId1)).thenReturn(Optional.of(activeEnrollment));

        TransferStudentRequest request = new TransferStudentRequest(
            studentId, sectionId1, sectionId2, LocalDate.of(2026, 8, 10)
        );

        assertThatThrownBy(() -> service.transferStudent(request))
            .isInstanceOf(InvalidBusinessOperationException.class)
            .hasMessageContaining("Transfer date cannot precede original enrollment start date");
    }

    @Test
    @DisplayName("Should return section enrollments for authorized faculty or admin")
    void getSectionEnrollments_Success() {
        Enrollment e1 = new Enrollment(studentId, sectionId1, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.empty());
        when(enrollmentPort.findBySection(sectionId1)).thenReturn(List.of(e1));

        List<EnrollmentResponse> responses = service.getSectionEnrollments(sectionId1);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).studentId()).isEqualTo(studentId);
        assertThat(responses.get(0).sectionId()).isEqualTo(sectionId1);
        assertThat(responses.get(0).status()).isEqualTo("ACTIVE");

        verify(authorizationService).requireFacultyOrAdmin("view section enrollments");
    }
}
