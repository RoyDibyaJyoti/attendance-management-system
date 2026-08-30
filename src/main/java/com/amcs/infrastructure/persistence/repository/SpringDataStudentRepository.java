package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStudentRepository extends JpaRepository<StudentEntity, UUID> {
    Optional<StudentEntity> findByRegistrationNumber(String registrationNumber);
    Optional<StudentEntity> findByEmail(String email);
}
