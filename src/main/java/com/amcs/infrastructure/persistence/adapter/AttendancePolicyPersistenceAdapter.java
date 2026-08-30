package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.infrastructure.persistence.entity.AttendancePolicyEntity;
import com.amcs.infrastructure.persistence.mapper.PolicyPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataAttendancePolicyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttendancePolicyPersistenceAdapter implements AttendancePolicyRepositoryPort {

    private final SpringDataAttendancePolicyRepository repository;
    private final PolicyPersistenceMapper mapper;

    public AttendancePolicyPersistenceAdapter(
        SpringDataAttendancePolicyRepository repository,
        PolicyPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public AttendancePolicy save(AttendancePolicy policy) {
        AttendancePolicyEntity entity = mapper.toEntity(policy, true);
        AttendancePolicyEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AttendancePolicy> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendancePolicy> findByNameAndVersion(String name, int version) {
        return repository.findByNameAndVersion(name, version).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendancePolicy> findActivePolicy(String name) {
        return repository.findActiveByName(name).map(mapper::toDomain);
    }

    @Override
    public List<AttendancePolicy> findAllVersions(String name) {
        return repository.findByNameOrderByVersionDesc(name)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
