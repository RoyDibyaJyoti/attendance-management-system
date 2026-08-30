package com.amcs.application.service;

import com.amcs.application.dto.student.CreateStudentRequest;
import com.amcs.application.dto.student.StudentResponse;
import com.amcs.application.dto.student.UpdateStudentRequest;
import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentApplicationServiceTest {

    @Mock private StudentRepositoryPort studentPort;
    @Mock private DepartmentRepositoryPort departmentPort;
    @Mock private com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    private StudentApplicationService service;

    private final UUID deptId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentApplicationService(studentPort, departmentPort, authorizationService);
    }

    @Test
    @DisplayName("Should create student successfully when valid")
    void shouldCreateStudentSuccessfully() {
        CreateStudentRequest request = new CreateStudentRequest("CS2026-001", "Alice Smith", "alice@univ.edu", deptId);

        when(departmentPort.findById(deptId)).thenReturn(Optional.of(new DepartmentEntity(deptId, "CS", "Computer Science")));
        when(studentPort.findByRegistrationNumber("CS2026-001")).thenReturn(Optional.empty());
        when(studentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = service.createStudent(request);

        assertThat(response).isNotNull();
        assertThat(response.registrationNumber()).isEqualTo("CS2026-001");
        assertThat(response.name()).isEqualTo("Alice Smith");
        assertThat(response.email()).isEqualTo("alice@univ.edu");
    }

    @Test
    @DisplayName("Duplicate registration number throws DuplicateResourceException")
    void shouldThrowWhenRegistrationNumberExists() {
        CreateStudentRequest request = new CreateStudentRequest("CS2026-001", "Alice Smith", "alice@univ.edu", deptId);

        when(departmentPort.findById(deptId)).thenReturn(Optional.of(new DepartmentEntity(deptId, "CS", "Computer Science")));
        when(studentPort.findByRegistrationNumber("CS2026-001")).thenReturn(Optional.of(new StudentEntity()));

        assertThatThrownBy(() -> service.createStudent(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Get by non-existent ID throws ResourceNotFoundException")
    void shouldThrowWhenStudentNotFound() {
        when(studentPort.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentById(studentId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update student contact info succeeds")
    void shouldUpdateStudentContactInfo() {
        StudentEntity existing = new StudentEntity(studentId, "CS2026-001", "Alice", "alice@univ.edu", deptId);
        UpdateStudentRequest request = new UpdateStudentRequest("Alice Wonderland", "alice.w@univ.edu");

        when(studentPort.findById(studentId)).thenReturn(Optional.of(existing));
        when(studentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StudentResponse response = service.updateStudent(studentId, request);

        assertThat(response.name()).isEqualTo("Alice Wonderland");
        assertThat(response.email()).isEqualTo("alice.w@univ.edu");
    }
}
