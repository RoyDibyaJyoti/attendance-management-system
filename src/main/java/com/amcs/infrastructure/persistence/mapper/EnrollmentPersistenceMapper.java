package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.enrollment.Enrollment;
import com.amcs.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class EnrollmentPersistenceMapper {

    public Enrollment toDomain(EnrollmentEntity entity, Optional<UUID> labGroupId) {
        if (entity == null) return null;
        return new Enrollment(
            entity.getStudentId(),
            entity.getSectionId(),
            entity.getEnrollmentStart(),
            Optional.ofNullable(entity.getEnrollmentEnd()),
            labGroupId
        );
    }

    public EnrollmentEntity toEntity(UUID id, Enrollment domain, String status) {
        if (domain == null) return null;
        return new EnrollmentEntity(
            id,
            domain.studentId(),
            domain.sectionId(),
            domain.enrollmentStart(),
            domain.enrollmentEnd().orElse(null),
            status != null ? status : "ACTIVE"
        );
    }
}
