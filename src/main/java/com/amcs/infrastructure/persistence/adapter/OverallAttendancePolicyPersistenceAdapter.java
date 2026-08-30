package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.OverallAttendancePolicyRepositoryPort;
import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataOverallAttendancePolicyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class OverallAttendancePolicyPersistenceAdapter implements OverallAttendancePolicyRepositoryPort {

    private final SpringDataOverallAttendancePolicyRepository repository;

    public OverallAttendancePolicyPersistenceAdapter(SpringDataOverallAttendancePolicyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public OverallAttendancePolicyEntity save(OverallAttendancePolicyEntity policy) {
        return repository.save(policy);
    }

    @Override
    public Optional<OverallAttendancePolicyEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<OverallAttendancePolicyEntity> findByNameAndVersion(String name, int version) {
        return repository.findByNameAndVersion(name, version);
    }

    @Override
    public Optional<OverallAttendancePolicyEntity> findActivePolicy(String name) {
        return repository.findActiveByName(name);
    }

    @Override
    public List<OverallAttendancePolicyEntity> findAllVersions(String name) {
        return repository.findByNameOrderByVersionDesc(name);
    }
}
