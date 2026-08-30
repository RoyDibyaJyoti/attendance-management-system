package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    @Query("SELECT e FROM EnrollmentEntity e WHERE e.studentId = :studentId AND e.sectionId = :sectionId AND e.status = 'ACTIVE'")
    Optional<EnrollmentEntity> findActiveByStudentIdAndSectionId(@Param("studentId") UUID studentId, @Param("sectionId") UUID sectionId);

    List<EnrollmentEntity> findByStudentId(UUID studentId);
    List<EnrollmentEntity> findBySectionId(UUID sectionId);
}
