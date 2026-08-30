package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AcademicPeriodPersistenceMapper {

    public AcademicPeriod toDomain(AcademicPeriodEntity entity) {
        if (entity == null) return null;
        return new AcademicPeriod(
            entity.getName(),
            entity.getStartDate(),
            entity.getEndDate()
        );
    }

    public AcademicPeriodEntity toEntity(UUID id, AcademicPeriod domain) {
        if (domain == null) return null;
        return new AcademicPeriodEntity(
            id,
            domain.name(),
            domain.startDate(),
            domain.endDate()
        );
    }
}
