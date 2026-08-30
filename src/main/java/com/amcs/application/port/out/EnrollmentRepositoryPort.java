package com.amcs.application.port.out;

import com.amcs.domain.enrollment.Enrollment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for student Enrollment domain entity.
 */
public interface EnrollmentRepositoryPort {
    Enrollment save(Enrollment enrollment);
    Optional<Enrollment> findActiveEnrollment(UUID studentId, UUID sectionId);
    List<Enrollment> findByStudent(UUID studentId);
    List<Enrollment> findBySection(UUID sectionId);
}
