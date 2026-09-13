package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.mapper.AcademicPeriodPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AcademicPeriodPersistenceAdapter implements AcademicPeriodRepositoryPort {

    private final SpringDataAcademicPeriodRepository repository;
    private final AcademicPeriodPersistenceMapper mapper;

    public AcademicPeriodPersistenceAdapter(
        SpringDataAcademicPeriodRepository repository,
        AcademicPeriodPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public AcademicPeriod save(UUID id, AcademicPeriod period) {
        AcademicPeriodEntity entity = mapper.toEntity(id, period);
        AcademicPeriodEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AcademicPeriod> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AcademicPeriod> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<AcademicPeriod> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AcademicPeriodEntity> findAllWithIds() {
        return repository.findAll();
    }
}
