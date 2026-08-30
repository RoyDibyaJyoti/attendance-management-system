package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOverallAttendancePolicyRepository extends JpaRepository<OverallAttendancePolicyEntity, UUID> {
    Optional<OverallAttendancePolicyEntity> findByNameAndVersion(String name, int version);

    @Query("SELECT p FROM OverallAttendancePolicyEntity p WHERE p.name = :name AND p.active = true")
    Optional<OverallAttendancePolicyEntity> findActiveByName(@Param("name") String name);

    List<OverallAttendancePolicyEntity> findByNameOrderByVersionDesc(String name);
}
