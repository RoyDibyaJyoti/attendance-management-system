package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.service.importer.payload.StagedStudentPayload;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentRowValidator Unit Tests")
class StudentRowValidatorTest {

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private DepartmentRepositoryPort departmentRepository;
    @Mock private SectionRepositoryPort sectionRepository;

    private StudentRowValidator validator;

    private final UUID deptId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private Set<String> seenRegNos;
    private Set<String> seenEmails;

    @BeforeEach
    void setUp() {
        validator = new StudentRowValidator(studentRepository, departmentRepository, sectionRepository);
        seenRegNos = new HashSet<>();
        seenEmails = new HashSet<>();
    }

    private ParsedRow createRow(String regNo, String name, String email, String dept, String section) {
        Map<String, String> map = Map.of(
            "registration number", regNo,
            "full name", name,
            "email", email,
            "department code", dept,
            "section name", section
        );
        return new ParsedRow(2, map, List.of(regNo, name, email, dept, section), false);
    }

    @Test
    @DisplayName("Valid student row passes and produces staged payload")
    void shouldValidateCleanStudentRow() {
        ParsedRow row = createRow("CS2026-001", "Alice Student", "alice@univ.edu", "CSE", "A");

        when(departmentRepository.findByCode("CSE"))
            .thenReturn(Optional.of(new DepartmentEntity(deptId, "CSE", "Computer Science")));
        when(sectionRepository.findAll())
            .thenReturn(List.of(new SectionEntity(sectionId, "A", deptId, UUID.randomUUID())));
        when(studentRepository.findByRegistrationNumber("CS2026-001")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("alice@univ.edu")).thenReturn(Optional.empty());

        ValidationOutcome<StagedStudentPayload> outcome = validator.validateRow(row, seenRegNos, seenEmails);

        assertThat(outcome.isValid()).isTrue();
        StagedStudentPayload payload = outcome.getPayload().orElseThrow();
        assertThat(payload.registrationNumber()).isEqualTo("CS2026-001");
        assertThat(payload.departmentId()).isEqualTo(deptId);
        assertThat(payload.sectionId()).isEqualTo(sectionId);
    }

    @Test
    @DisplayName("Invalid email format produces structured error")
    void shouldRejectInvalidEmail() {
        ParsedRow row = createRow("CS2026-002", "Bob Student", "invalid-email", "CSE", "A");

        ValidationOutcome<StagedStudentPayload> outcome = validator.validateRow(row, seenRegNos, seenEmails);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("INVALID_EMAIL");
    }

    @Test
    @DisplayName("Non-existent department produces DEPARTMENT_NOT_FOUND error")
    void shouldRejectUnknownDepartment() {
        ParsedRow row = createRow("CS2026-003", "Charlie", "charlie@univ.edu", "UNKNOWN_DEPT", "A");
        when(departmentRepository.findByCode("UNKNOWN_DEPT")).thenReturn(Optional.empty());

        ValidationOutcome<StagedStudentPayload> outcome = validator.validateRow(row, seenRegNos, seenEmails);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("DEPARTMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("Duplicate registration number in same file is rejected")
    void shouldRejectDuplicateInFile() {
        ParsedRow row1 = createRow("CS2026-004", "David", "david@univ.edu", "CSE", "A");
        ParsedRow row2 = createRow("CS2026-004", "David Copy", "david2@univ.edu", "CSE", "A");

        when(departmentRepository.findByCode("CSE"))
            .thenReturn(Optional.of(new DepartmentEntity(deptId, "CSE", "Computer Science")));
        when(sectionRepository.findAll())
            .thenReturn(List.of(new SectionEntity(sectionId, "A", deptId, UUID.randomUUID())));

        validator.validateRow(row1, seenRegNos, seenEmails);
        ValidationOutcome<StagedStudentPayload> outcome2 = validator.validateRow(row2, seenRegNos, seenEmails);

        assertThat(outcome2.isValid()).isFalse();
        assertThat(outcome2.getError().orElseThrow().errorCode()).isEqualTo("DUPLICATE_IN_FILE");
    }

    @Test
    @DisplayName("Existing registration number in DB is rejected")
    void shouldRejectExistingStudentInDb() {
        ParsedRow row = createRow("CS2026-005", "Eve", "eve@univ.edu", "CSE", "A");

        when(departmentRepository.findByCode("CSE"))
            .thenReturn(Optional.of(new DepartmentEntity(deptId, "CSE", "Computer Science")));
        when(sectionRepository.findAll())
            .thenReturn(List.of(new SectionEntity(sectionId, "A", deptId, UUID.randomUUID())));
        when(studentRepository.findByRegistrationNumber("CS2026-005"))
            .thenReturn(Optional.of(new StudentEntity(UUID.randomUUID(), "CS2026-005", "Eve Existing", "eve@univ.edu", deptId)));

        ValidationOutcome<StagedStudentPayload> outcome = validator.validateRow(row, seenRegNos, seenEmails);

        assertThat(outcome.isValid()).isFalse();
        assertThat(outcome.getError().orElseThrow().errorCode()).isEqualTo("STUDENT_ALREADY_EXISTS");
    }
}
