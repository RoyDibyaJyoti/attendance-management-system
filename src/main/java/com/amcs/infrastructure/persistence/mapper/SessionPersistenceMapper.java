package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.infrastructure.persistence.entity.SessionEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SessionPersistenceMapper {

    public Session toDomain(SessionEntity entity) {
        if (entity == null) return null;
        return new Session(
            entity.getId(),
            entity.getSubjectId(),
            entity.getSectionId(),
            entity.getConductedByFacultyId(),
            entity.getSessionDate(),
            SessionType.valueOf(entity.getSessionType()),
            entity.getPlannedUnits(),
            entity.getConductedUnits(),
            SessionStatus.valueOf(entity.getStatus()),
            Optional.ofNullable(entity.getLabGroupId()),
            Optional.ofNullable(entity.getReplacedBySessionId())
        );
    }

    public SessionEntity toEntity(Session domain, UUID academicPeriodId) {
        if (domain == null) return null;
        return new SessionEntity(
            domain.id(),
            domain.subjectId(),
            domain.sectionId(),
            domain.conductedByFacultyId(),
            academicPeriodId,
            domain.sessionDate(),
            domain.sessionType().name(),
            domain.plannedUnits(),
            domain.conductedUnits(),
            domain.status().name(),
            domain.labGroupId().orElse(null),
            domain.replacedBySessionId().orElse(null)
        );
    }
}
