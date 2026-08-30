package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentPersistenceAdapter implements StudentRepositoryPort {

    private final SpringDataStudentRepository repository;

    public StudentPersistenceAdapter(SpringDataStudentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public StudentEntity save(StudentEntity student) {
        return repository.save(student);
    }

    @Override
    public Optional<StudentEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<StudentEntity> findByRegistrationNumber(String registrationNumber) {
        return repository.findByRegistrationNumber(registrationNumber);
    }

    @Override
    public Optional<StudentEntity> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public List<StudentEntity> findAll() {
        return repository.findAll();
    }
}
