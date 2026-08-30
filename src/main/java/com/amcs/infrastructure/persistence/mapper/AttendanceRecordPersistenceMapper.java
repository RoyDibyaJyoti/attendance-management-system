package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.infrastructure.persistence.entity.AttendanceRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRecordPersistenceMapper {

    public AttendanceRecord toDomain(AttendanceRecordEntity entity) {
        if (entity == null) return null;
        return new AttendanceRecord(
            entity.getId(),
            entity.getSessionId(),
            entity.getStudentId(),
            AttendanceStatus.valueOf(entity.getStatus())
        );
    }

    public AttendanceRecordEntity toEntity(AttendanceRecord domain) {
        if (domain == null) return null;
        return new AttendanceRecordEntity(
            domain.id(),
            domain.sessionId(),
            domain.studentId(),
            domain.status().name()
        );
    }
}
