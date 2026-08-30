package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.SectionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectionRepositoryPort {
    SectionEntity save(SectionEntity section);
    Optional<SectionEntity> findById(UUID id);
    List<SectionEntity> findByAcademicPeriod(UUID academicPeriodId);
    List<SectionEntity> findAll();
}
