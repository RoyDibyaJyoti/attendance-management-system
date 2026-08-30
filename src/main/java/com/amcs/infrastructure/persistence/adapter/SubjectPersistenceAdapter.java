package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.domain.academic.Subject;
import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import com.amcs.infrastructure.persistence.mapper.SubjectPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataSubjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubjectPersistenceAdapter implements SubjectRepositoryPort {

    private final SpringDataSubjectRepository repository;
    private final SubjectPersistenceMapper mapper;

    public SubjectPersistenceAdapter(SpringDataSubjectRepository repository, SubjectPersistenceMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Subject save(Subject subject, UUID departmentId) {
        SubjectEntity entity = mapper.toEntity(subject, departmentId);
        SubjectEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Subject> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Subject> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
