package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class DepartmentPersistenceAdapter implements DepartmentRepositoryPort {

    private final SpringDataDepartmentRepository repository;

    public DepartmentPersistenceAdapter(SpringDataDepartmentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public DepartmentEntity save(DepartmentEntity department) {
        return repository.save(department);
    }

    @Override
    public Optional<DepartmentEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<DepartmentEntity> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Override
    public List<DepartmentEntity> findAll() {
        return repository.findAll();
    }
}
