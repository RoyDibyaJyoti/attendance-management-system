package com.amcs.infrastructure.persistence.adapter;

import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.ImportJobEntity;
import com.amcs.infrastructure.persistence.entity.ImportJobErrorEntity;
import com.amcs.infrastructure.persistence.mapper.ImportJobPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataImportJobErrorRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportJobPersistenceAdapter Unit Tests")
class ImportJobPersistenceAdapterTest {

    @Mock private SpringDataImportJobRepository jobRepository;
    @Mock private SpringDataImportJobErrorRepository errorRepository;

    private ImportJobPersistenceMapper mapper;
    private ImportJobPersistenceAdapter adapter;

    private final UUID jobId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-30T11:00:00Z");

    @BeforeEach
    void setUp() {
        mapper = new ImportJobPersistenceMapper();
        adapter = new ImportJobPersistenceAdapter(jobRepository, errorRepository, mapper);
    }

    @Test
    @DisplayName("Should save new ImportJob with staged errors")
    void shouldSaveNewImportJobWithErrors() {
        ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "students.xlsx", userId, now);
        RowValidationError error = new RowValidationError(2, "email", "bad", "INVALID_EMAIL", "Malformed email");
        job.stage(10, 9, List.of(error));

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());
        when(jobRepository.save(any(ImportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportJob saved = adapter.save(job);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(jobId);
        assertThat(saved.getStatus()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(saved.getTotalRows()).isEqualTo(10);
        assertThat(saved.getValidRows()).isEqualTo(9);
        assertThat(saved.getInvalidRows()).isEqualTo(1);
        assertThat(saved.getErrors()).hasSize(1);
        assertThat(saved.getErrors().getFirst().errorCode()).isEqualTo("INVALID_EMAIL");

        verify(jobRepository).save(any(ImportJobEntity.class));
    }

    @Test
    @DisplayName("Should find ImportJob by ID with errors loaded and reconstituted")
    void shouldFindByIdWithErrors() {
        ImportJobEntity entity = new ImportJobEntity(
            jobId,
            "SESSIONS",
            "FAIL_FAST",
            "STAGED_CLEAN",
            "sessions.xlsx",
            20,
            20,
            0,
            userId,
            now,
            null,
            null
        );

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(entity));
        when(errorRepository.findByJobIdOrderByRowIndexAsc(jobId)).thenReturn(List.of());

        Optional<ImportJob> result = adapter.findById(jobId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(jobId);
        assertThat(result.get().getImportType()).isEqualTo(ImportType.SESSIONS);
        assertThat(result.get().getStatus()).isEqualTo(ImportStatus.STAGED_CLEAN);
        assertThat(result.get().getTotalRows()).isEqualTo(20);
        assertThat(result.get().getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should update existing ImportJob when committed")
    void shouldUpdateExistingJobWhenCommitted() {
        ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
        job.stage(5, 5, List.of());
        Instant commitTime = now.plusSeconds(45);
        job.commit(commitTime);

        ImportJobEntity existingEntity = new ImportJobEntity(
            jobId, "STUDENTS", "FAIL_FAST", "STAGED_CLEAN", "file.xlsx", 5, 5, 0, userId, now, null, null
        );

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(existingEntity));
        when(jobRepository.save(any(ImportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportJob updated = adapter.save(job);

        assertThat(updated.getStatus()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(updated.getCommittedAt()).isEqualTo(commitTime);
    }

    @Test
    @DisplayName("Should delete job by ID")
    void shouldDeleteJobById() {
        adapter.deleteById(jobId);
        verify(jobRepository).deleteById(jobId);
    }
}
