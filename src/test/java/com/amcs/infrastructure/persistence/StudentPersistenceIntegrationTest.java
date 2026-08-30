package com.amcs.infrastructure.persistence;

import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Student Persistence Integration Tests")
class StudentPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataDepartmentRepository departmentRepository;

    @Test
    @Transactional
    @DisplayName("ST-01: Create and retrieve student by ID and registration number")
    void createAndRetrieveStudent() {
        DepartmentEntity dept = departmentRepository.save(
            new DepartmentEntity(UUID.randomUUID(), "CSE_" + UUID.randomUUID().toString().substring(0, 8), "Computer Science"));

        UUID studentId = UUID.randomUUID();
        String regNo = "REG_" + UUID.randomUUID().toString().substring(0, 8);
        StudentEntity student = new StudentEntity(
            studentId, regNo, "Alice Student", regNo + "@university.edu", dept.getId());

        studentRepository.save(student);

        Optional<StudentEntity> foundById = studentRepository.findById(studentId);
        assertThat(foundById).isPresent();
        assertThat(foundById.get().getName()).isEqualTo("Alice Student");
        assertThat(foundById.get().getRegistrationNumber()).isEqualTo(regNo);

        Optional<StudentEntity> foundByReg = studentRepository.findByRegistrationNumber(regNo);
        assertThat(foundByReg).isPresent();
        assertThat(foundByReg.get().getId()).isEqualTo(studentId);
    }

    @Test
    @DisplayName("ST-02: Duplicate registration number is rejected by database unique constraint")
    void duplicateRegistrationNumber_rejected() {
        DepartmentEntity dept = departmentRepository.save(
            new DepartmentEntity(UUID.randomUUID(), "ECE_" + UUID.randomUUID().toString().substring(0, 8), "Electronics"));

        String duplicateReg = "REG_DUP_" + UUID.randomUUID().toString().substring(0, 8);

        StudentEntity s1 = new StudentEntity(
            UUID.randomUUID(), duplicateReg, "Student One", "s1_" + duplicateReg + "@test.edu", dept.getId());
        studentRepository.saveAndFlush(s1);

        StudentEntity s2 = new StudentEntity(
            UUID.randomUUID(), duplicateReg, "Student Two", "s2_" + duplicateReg + "@test.edu", dept.getId());

        assertThatThrownBy(() -> studentRepository.saveAndFlush(s2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
