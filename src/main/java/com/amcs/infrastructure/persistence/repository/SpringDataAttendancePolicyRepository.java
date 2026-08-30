package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.AttendancePolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAttendancePolicyRepository extends JpaRepository<AttendancePolicyEntity, UUID> {
    Optional<AttendancePolicyEntity> findByNameAndVersion(String name, int version);

    @Query("SELECT p FROM AttendancePolicyEntity p WHERE p.name = :name AND p.active = true")
    Optional<AttendancePolicyEntity> findActiveByName(@Param("name") String name);

    List<AttendancePolicyEntity> findByNameOrderByVersionDesc(String name);
}
