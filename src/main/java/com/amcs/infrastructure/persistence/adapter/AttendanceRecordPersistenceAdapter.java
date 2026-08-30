package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.infrastructure.persistence.entity.AttendanceRecordEntity;
import com.amcs.infrastructure.persistence.mapper.AttendanceRecordPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataAttendanceRecordRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttendanceRecordPersistenceAdapter implements AttendanceRecordRepositoryPort {

    private final SpringDataAttendanceRecordRepository repository;
    private final AttendanceRecordPersistenceMapper mapper;

    public AttendanceRecordPersistenceAdapter(
        SpringDataAttendanceRecordRepository repository,
        AttendanceRecordPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public AttendanceRecord save(AttendanceRecord record) {
        AttendanceRecordEntity entity = mapper.toEntity(record);
        AttendanceRecordEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AttendanceRecord> saveAll(List<AttendanceRecord> records) {
        List<AttendanceRecordEntity> entities = records.stream()
            .map(mapper::toEntity)
            .toList();
        return repository.saveAll(entities).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AttendanceRecord> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendanceRecord> findBySessionAndStudent(UUID sessionId, UUID studentId) {
        return repository.findBySessionIdAndStudentId(sessionId, studentId).map(mapper::toDomain);
    }

    @Override
    public List<AttendanceRecord> findByStudentAndSessions(UUID studentId, List<UUID> sessionIds) {
        if (sessionIds.isEmpty()) return List.of();
        return repository.findByStudentIdAndSessionIdIn(studentId, sessionIds)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<AttendanceRecord> findBySession(UUID sessionId) {
        return repository.findBySessionId(sessionId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
