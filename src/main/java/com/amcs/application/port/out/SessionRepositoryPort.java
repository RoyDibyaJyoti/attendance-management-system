package com.amcs.application.port.out;

import com.amcs.domain.attendance.Session;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for Session domain entity.
 */
public interface SessionRepositoryPort {
    Session save(Session session, UUID academicPeriodId);
    Optional<Session> findById(UUID id);
    List<Session> findBySubjectAndPeriod(UUID subjectId, UUID academicPeriodId);
    List<Session> findBySection(UUID sectionId);
}
