package com.amcs.application.port.out;

import com.amcs.domain.academic.Subject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for Subject domain entity.
 */
public interface SubjectRepositoryPort {
    Subject save(Subject subject, UUID departmentId);
    Optional<Subject> findById(UUID id);
    Optional<Subject> findByCode(String code);
    List<Subject> findAll();
}
