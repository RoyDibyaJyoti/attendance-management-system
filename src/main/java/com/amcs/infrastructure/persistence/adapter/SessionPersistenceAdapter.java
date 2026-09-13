package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.domain.attendance.Session;
import com.amcs.infrastructure.persistence.entity.SessionEntity;
import com.amcs.infrastructure.persistence.mapper.SessionPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataSessionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionPersistenceAdapter implements SessionRepositoryPort {

    private final SpringDataSessionRepository repository;
    private final SessionPersistenceMapper mapper;

    public SessionPersistenceAdapter(SpringDataSessionRepository repository, SessionPersistenceMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Session save(Session session, UUID academicPeriodId) {
        Optional<SessionEntity> existing = repository.findById(session.id());
        SessionEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setSubjectId(session.subjectId());
            entity.setSectionId(session.sectionId());
            entity.setConductedByFacultyId(session.conductedByFacultyId());
            entity.setAcademicPeriodId(academicPeriodId);
            entity.setSessionDate(session.sessionDate());
            entity.setSessionType(session.sessionType().name());
            entity.setPlannedUnits(session.plannedUnits());
            entity.setConductedUnits(session.conductedUnits());
            entity.setStatus(session.status().name());
            entity.setLabGroupId(session.labGroupId().orElse(null));
            entity.setReplacedBySessionId(session.replacedBySessionId().orElse(null));
            entity.setUpdatedAt(java.time.Instant.now());
        } else {
            entity = mapper.toEntity(session, academicPeriodId);
        }
        SessionEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Session> findBySubjectAndPeriod(UUID subjectId, UUID academicPeriodId) {
        return repository.findBySubjectIdAndAcademicPeriodId(subjectId, academicPeriodId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Session> findBySection(UUID sectionId) {
        return repository.findBySectionId(sectionId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
