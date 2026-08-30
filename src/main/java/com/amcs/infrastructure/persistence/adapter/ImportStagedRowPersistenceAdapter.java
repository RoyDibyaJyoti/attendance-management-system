package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.importer.ImportStagedRow;
import com.amcs.application.port.out.importer.ImportStagedRowRepositoryPort;
import com.amcs.infrastructure.persistence.entity.ImportStagedRowEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataImportStagedRowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ImportStagedRowPersistenceAdapter implements ImportStagedRowRepositoryPort {

    private final SpringDataImportStagedRowRepository repository;

    public ImportStagedRowPersistenceAdapter(SpringDataImportStagedRowRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    @Transactional
    public void saveAll(List<ImportStagedRow> stagedRows) {
        if (stagedRows == null || stagedRows.isEmpty()) {
            return;
        }

        List<ImportStagedRowEntity> entities = stagedRows.stream()
            .map(row -> new ImportStagedRowEntity(
                row.id(),
                row.jobId(),
                row.rowIndex(),
                row.rowType(),
                row.payloadJson()
            ))
            .collect(Collectors.toList());

        repository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportStagedRow> findByJobId(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        return repository.findByJobIdOrderByRowIndexAsc(jobId).stream()
            .map(e -> new ImportStagedRow(e.getId(), e.getJobId(), e.getRowIndex(), e.getRowType(), e.getPayloadJson()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByJobId(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        repository.deleteByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByJobId(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        return repository.countByJobId(jobId);
    }
}
