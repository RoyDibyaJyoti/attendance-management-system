package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.StudentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for student data.
 */
public interface StudentRepositoryPort {
    StudentEntity save(StudentEntity student);
    Optional<StudentEntity> findById(UUID id);
    Optional<StudentEntity> findByRegistrationNumber(String registrationNumber);
    Optional<StudentEntity> findByEmail(String email);
    List<StudentEntity> findAll();
}
