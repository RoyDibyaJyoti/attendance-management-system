package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataLabGroupRepository extends JpaRepository<LabGroupEntity, UUID> {
    Optional<LabGroupEntity> findBySectionIdAndName(UUID sectionId, String name);
    List<LabGroupEntity> findBySectionId(UUID sectionId);
}
