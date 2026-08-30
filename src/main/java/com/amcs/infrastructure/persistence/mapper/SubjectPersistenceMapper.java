package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SubjectPersistenceMapper {

    public Subject toDomain(SubjectEntity entity) {
        if (entity == null) return null;
        return new Subject(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            CourseType.valueOf(entity.getCourseType()),
            entity.getCreditHours()
        );
    }

    public SubjectEntity toEntity(Subject domain, UUID departmentId) {
        if (domain == null) return null;
        return new SubjectEntity(
            domain.id(),
            domain.code(),
            domain.name(),
            domain.courseType().name(),
            domain.creditHours(),
            departmentId
        );
    }
}
