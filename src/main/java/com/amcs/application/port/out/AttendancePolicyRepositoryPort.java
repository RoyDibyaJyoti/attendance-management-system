package com.amcs.application.port.out;

import com.amcs.domain.policy.AttendancePolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for AttendancePolicy domain entity.
 */
public interface AttendancePolicyRepositoryPort {
    AttendancePolicy save(AttendancePolicy policy);
    Optional<AttendancePolicy> findById(UUID id);
    Optional<AttendancePolicy> findByNameAndVersion(String name, int version);
    Optional<AttendancePolicy> findActivePolicy(String name);
    List<AttendancePolicy> findAllVersions(String name);
}
