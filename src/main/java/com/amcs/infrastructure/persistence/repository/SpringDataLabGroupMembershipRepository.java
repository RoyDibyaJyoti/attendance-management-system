package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataLabGroupMembershipRepository extends JpaRepository<LabGroupMembershipEntity, UUID> {

    @Query("SELECT m FROM LabGroupMembershipEntity m WHERE m.studentId = :studentId " +
           "AND m.effectiveStart <= :date AND (m.effectiveEnd IS NULL OR m.effectiveEnd >= :date)")
    Optional<LabGroupMembershipEntity> findActiveByStudentIdOnDate(@Param("studentId") UUID studentId, @Param("date") LocalDate date);

    List<LabGroupMembershipEntity> findByStudentId(UUID studentId);
}
