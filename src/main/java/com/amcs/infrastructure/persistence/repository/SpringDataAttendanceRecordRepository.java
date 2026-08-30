package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.AttendanceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, UUID> {
    Optional<AttendanceRecordEntity> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);
    List<AttendanceRecordEntity> findByStudentIdAndSessionIdIn(UUID studentId, List<UUID> sessionIds);
    List<AttendanceRecordEntity> findBySessionId(UUID sessionId);
}
