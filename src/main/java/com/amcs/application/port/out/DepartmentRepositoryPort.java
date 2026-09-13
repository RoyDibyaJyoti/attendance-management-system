package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepositoryPort {
    DepartmentEntity save(DepartmentEntity department);
    Optional<DepartmentEntity> findById(UUID id);
    Optional<DepartmentEntity> findByCode(String code);
    List<DepartmentEntity> findAll();
    List<DepartmentEntity> findByIsActive(boolean isActive);
}
