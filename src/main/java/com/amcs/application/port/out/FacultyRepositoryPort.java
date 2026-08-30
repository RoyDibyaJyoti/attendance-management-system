package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultyRepositoryPort {
    FacultyEntity save(FacultyEntity faculty);
    Optional<FacultyEntity> findById(UUID id);
    Optional<FacultyEntity> findByEmployeeId(String employeeId);
    List<FacultyEntity> findAll();
    List<FacultyEntity> findByDepartment(UUID departmentId);
}
