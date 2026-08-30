package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSectionRepository extends JpaRepository<SectionEntity, UUID> {
    Optional<SectionEntity> findByDepartmentIdAndAcademicPeriodIdAndName(UUID departmentId, UUID academicPeriodId, String name);
    List<SectionEntity> findByAcademicPeriodId(UUID academicPeriodId);
}
