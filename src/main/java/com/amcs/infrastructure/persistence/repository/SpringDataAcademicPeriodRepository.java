package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, UUID> {
    Optional<AcademicPeriodEntity> findByName(String name);
}
