package com.amcs.infrastructure.persistence.mapper;

import com.amcs.application.port.out.security.FacultyAssignment;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.FacultyAssignmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import org.springframework.stereotype.Component;

@Component
public class FacultyAssignmentPersistenceMapper {

    public FacultyAssignment toDomain(FacultyAssignmentEntity entity) {
        if (entity == null) return null;
        return new FacultyAssignment(
            entity.getId(),
            entity.getFacultyId(),
            entity.getSubjectId(),
            entity.getSectionId(),
            entity.getAcademicPeriodId(),
            entity.getAssignmentStart(),
            entity.getAssignmentEnd(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    public FacultyAssignmentEntity toEntity(
        FacultyAssignment domain,
        FacultyEntity faculty,
        SubjectEntity subject,
        SectionEntity section,
        AcademicPeriodEntity period
    ) {
        if (domain == null) return null;
        return new FacultyAssignmentEntity(
            domain.id(),
            faculty,
            subject,
            section,
            period,
            domain.assignmentStart(),
            domain.assignmentEnd(),
            domain.status()
        );
    }
}
