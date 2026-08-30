package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class SectionPersistenceAdapter implements SectionRepositoryPort {

    private final SpringDataSectionRepository repository;

    public SectionPersistenceAdapter(SpringDataSectionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public SectionEntity save(SectionEntity section) {
        return repository.save(section);
    }

    @Override
    public Optional<SectionEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<SectionEntity> findByAcademicPeriod(UUID academicPeriodId) {
        return repository.findByAcademicPeriodId(academicPeriodId);
    }

    @Override
    public List<SectionEntity> findAll() {
        return repository.findAll();
    }
}
