package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataFacultyRepository extends JpaRepository<FacultyEntity, UUID> {
    Optional<FacultyEntity> findByEmployeeId(String employeeId);
    Optional<FacultyEntity> findByEmail(String email);
}
