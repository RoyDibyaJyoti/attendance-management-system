package com.amcs.application.port.out;

import com.amcs.domain.attendance.AttendanceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for AttendanceRecord domain entity.
 */
public interface AttendanceRecordRepositoryPort {
    AttendanceRecord save(AttendanceRecord record);
    List<AttendanceRecord> saveAll(List<AttendanceRecord> records);
    Optional<AttendanceRecord> findById(UUID id);
    Optional<AttendanceRecord> findBySessionAndStudent(UUID sessionId, UUID studentId);
    List<AttendanceRecord> findByStudentAndSessions(UUID studentId, List<UUID> sessionIds);
    List<AttendanceRecord> findBySession(UUID sessionId);
}
