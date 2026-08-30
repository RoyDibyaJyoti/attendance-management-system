package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.ImportJobEntity;
import com.amcs.infrastructure.persistence.mapper.ImportJobPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataImportJobErrorRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataImportJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ImportJobPersistenceAdapter implements ImportJobRepositoryPort {

    private final SpringDataImportJobRepository jobRepository;
    private final SpringDataImportJobErrorRepository errorRepository;
    private final ImportJobPersistenceMapper mapper;

    public ImportJobPersistenceAdapter(
        SpringDataImportJobRepository jobRepository,
        SpringDataImportJobErrorRepository errorRepository,
        ImportJobPersistenceMapper mapper
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository must not be null");
        this.errorRepository = Objects.requireNonNull(errorRepository, "errorRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public ImportJob save(ImportJob domain) {
        Objects.requireNonNull(domain, "domain ImportJob must not be null");

        Optional<ImportJobEntity> existingOpt = jobRepository.findById(domain.getId());
        ImportJobEntity entityToSave;

        if (existingOpt.isPresent()) {
            entityToSave = existingOpt.get();
            entityToSave.setStatus(domain.getStatus().name());
            entityToSave.setTotalRows(domain.getTotalRows());
            entityToSave.setValidRows(domain.getValidRows());
            entityToSave.setInvalidRows(domain.getInvalidRows());
            entityToSave.setCommittedAt(domain.getCommittedAt());
            entityToSave.setDiscardedAt(domain.getDiscardedAt());

            entityToSave.getErrors().clear();
            if (domain.getErrors() != null) {
                for (RowValidationError error : domain.getErrors()) {
                    entityToSave.addError(mapper.toErrorEntity(error, entityToSave));
                }
            }
        } else {
            entityToSave = mapper.toEntity(domain);
        }

        ImportJobEntity saved = jobRepository.save(entityToSave);
        return mapper.toDomain(saved, saved.getErrors());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJob> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return jobRepository.findById(id)
            .map(entity -> mapper.toDomain(entity, errorRepository.findByJobIdOrderByRowIndexAsc(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJob> findByCreatedByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return jobRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(entity -> mapper.toDomain(entity, errorRepository.findByJobIdOrderByRowIndexAsc(entity.getId())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJob> findByStatus(ImportStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return jobRepository.findByStatusOrderByCreatedAtDesc(status.name()).stream()
            .map(entity -> mapper.toDomain(entity, errorRepository.findByJobIdOrderByRowIndexAsc(entity.getId())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJob> findByImportType(ImportType type) {
        Objects.requireNonNull(type, "type must not be null");
        return jobRepository.findByImportTypeOrderByCreatedAtDesc(type.name()).stream()
            .map(entity -> mapper.toDomain(entity, errorRepository.findByJobIdOrderByRowIndexAsc(entity.getId())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        jobRepository.deleteById(id);
    }
}
