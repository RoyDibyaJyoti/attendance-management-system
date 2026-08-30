package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OverallAttendancePolicyRepositoryPort {
    OverallAttendancePolicyEntity save(OverallAttendancePolicyEntity policy);
    Optional<OverallAttendancePolicyEntity> findById(UUID id);
    Optional<OverallAttendancePolicyEntity> findByNameAndVersion(String name, int version);
    Optional<OverallAttendancePolicyEntity> findActivePolicy(String name);
    List<OverallAttendancePolicyEntity> findAllVersions(String name);
}
