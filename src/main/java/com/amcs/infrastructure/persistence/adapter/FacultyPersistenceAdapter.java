package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataFacultyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class FacultyPersistenceAdapter implements FacultyRepositoryPort {

    private final SpringDataFacultyRepository repository;

    public FacultyPersistenceAdapter(SpringDataFacultyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public FacultyEntity save(FacultyEntity faculty) {
        return repository.save(faculty);
    }

    @Override
    public Optional<FacultyEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<FacultyEntity> findByEmployeeId(String employeeId) {
        return repository.findByEmployeeId(employeeId);
    }

    @Override
    public List<FacultyEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public List<FacultyEntity> findByDepartment(UUID departmentId) {
        return repository.findAll().stream()
            .filter(f -> f.getDepartmentId().equals(departmentId))
            .toList();
    }
}
