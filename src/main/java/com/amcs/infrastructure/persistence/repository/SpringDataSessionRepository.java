package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataSessionRepository extends JpaRepository<SessionEntity, UUID> {
    List<SessionEntity> findBySubjectIdAndAcademicPeriodId(UUID subjectId, UUID academicPeriodId);
    List<SessionEntity> findBySectionId(UUID sectionId);
}
