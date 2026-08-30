package com.amcs.infrastructure.persistence;

import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.ImportJobErrorEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataImportJobErrorRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportJob PostgreSQL Persistence Integration Tests")
class ImportJobPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private ImportJobRepositoryPort importJobRepository;

    @Autowired
    private SpringDataUserAccountRepository userAccountRepository;

    @Autowired
    private SpringDataImportJobErrorRepository errorRepository;

    private UUID testUserId;

    @BeforeEach
    void setUpUser() {
        testUserId = UUID.randomUUID();
        UserAccountEntity user = new UserAccountEntity(
            testUserId,
            "import_admin_" + testUserId.toString().substring(0, 8),
            "admin_" + testUserId.toString().substring(0, 8) + "@univ.edu",
            "$2a$12$DummyPasswordHashForImportTests1234567890",
            "HOD_ADMIN",
            null,
            null,
            "ACTIVE",
            0,
            null,
            1
        );
        userAccountRepository.save(user);
    }

    @Test
    @Transactional
    @DisplayName("IJ-01: Save and reload clean staged job with all fields intact")
    void shouldPersistAndReloadCleanStagedJob() {
        UUID jobId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        ImportJob job = ImportJob.create(
            jobId,
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "students_fall_2026.xlsx",
            testUserId,
            createdAt
        );
        job.stage(150, 150, List.of());

        importJobRepository.save(job);

        Optional<ImportJob> loadedOpt = importJobRepository.findById(jobId);
        assertThat(loadedOpt).isPresent();

        ImportJob loaded = loadedOpt.get();
        assertThat(loaded.getId()).isEqualTo(jobId);
        assertThat(loaded.getImportType()).isEqualTo(ImportType.STUDENTS);
        assertThat(loaded.getImportMode()).isEqualTo(ImportMode.FAIL_FAST);
        assertThat(loaded.getStatus()).isEqualTo(ImportStatus.STAGED_CLEAN);
        assertThat(loaded.getOriginalFilename()).isEqualTo("students_fall_2026.xlsx");
        assertThat(loaded.getTotalRows()).isEqualTo(150);
        assertThat(loaded.getValidRows()).isEqualTo(150);
        assertThat(loaded.getInvalidRows()).isZero();
        assertThat(loaded.getErrors()).isEmpty();
        assertThat(loaded.getCreatedByUserId()).isEqualTo(testUserId);
        assertThat(loaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(loaded.getCommittedAt()).isNull();
        assertThat(loaded.getDiscardedAt()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("IJ-02: Persist job with multiple validation errors and reload in row order")
    void shouldPersistAndReloadJobWithErrors() {
        UUID jobId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        ImportJob job = ImportJob.create(
            jobId,
            ImportType.ATTENDANCE_RECORDS,
            ImportMode.PARTIAL_COMMIT,
            "attendance_sheet.xlsx",
            testUserId,
            createdAt
        );

        RowValidationError error1 = new RowValidationError(2, "status", "UNKNOWN", "INVALID_STATUS", "Unrecognized attendance status");
        RowValidationError error2 = new RowValidationError(5, "studentId", "none", "STUDENT_NOT_FOUND", "Student does not exist");
        RowValidationError error3 = new RowValidationError(14, "sessionDate", "2020-01-01", "OUTSIDE_PERIOD", "Date is prior to semester start");

        job.stage(50, 47, List.of(error3, error1, error2)); // Added out of order

        importJobRepository.save(job);

        Optional<ImportJob> loadedOpt = importJobRepository.findById(jobId);
        assertThat(loadedOpt).isPresent();

        ImportJob loaded = loadedOpt.get();
        assertThat(loaded.getStatus()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(loaded.getTotalRows()).isEqualTo(50);
        assertThat(loaded.getValidRows()).isEqualTo(47);
        assertThat(loaded.getInvalidRows()).isEqualTo(3);
        assertThat(loaded.getErrors()).hasSize(3);

        // Verification of ascending row index ordering
        assertThat(loaded.getErrors().get(0).rowIndex()).isEqualTo(2);
        assertThat(loaded.getErrors().get(0).columnName()).isEqualTo("status");
        assertThat(loaded.getErrors().get(0).rejectedValue()).isEqualTo("UNKNOWN");
        assertThat(loaded.getErrors().get(0).errorCode()).isEqualTo("INVALID_STATUS");

        assertThat(loaded.getErrors().get(1).rowIndex()).isEqualTo(5);
        assertThat(loaded.getErrors().get(1).columnName()).isEqualTo("studentId");
        assertThat(loaded.getErrors().get(1).errorCode()).isEqualTo("STUDENT_NOT_FOUND");

        assertThat(loaded.getErrors().get(2).rowIndex()).isEqualTo(14);
        assertThat(loaded.getErrors().get(2).columnName()).isEqualTo("sessionDate");
        assertThat(loaded.getErrors().get(2).errorCode()).isEqualTo("OUTSIDE_PERIOD");
    }

    @Test
    @Transactional
    @DisplayName("IJ-03: Transition job lifecycle through STAGED_CLEAN -> COMMITTED")
    void shouldPersistLifecycleTransitionToCommitted() {
        UUID jobId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        ImportJob job = ImportJob.create(
            jobId,
            ImportType.SESSIONS,
            ImportMode.FAIL_FAST,
            "sessions.xlsx",
            testUserId,
            createdAt
        );
        job.stage(25, 25, List.of());
        importJobRepository.save(job);

        Instant committedAt = createdAt.plusSeconds(30);
        job.commit(committedAt);
        importJobRepository.save(job);

        ImportJob reloaded = importJobRepository.findById(jobId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(reloaded.getCommittedAt()).isEqualTo(committedAt);
        assertThat(reloaded.isTerminal()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("IJ-04: Transition job lifecycle through STAGED_PARTIAL -> DISCARDED")
    void shouldPersistLifecycleTransitionToDiscarded() {
        UUID jobId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        ImportJob job = ImportJob.create(
            jobId,
            ImportType.STUDENTS,
            ImportMode.PARTIAL_COMMIT,
            "students_batch.xlsx",
            testUserId,
            createdAt
        );
        job.stage(10, 8, List.of(new RowValidationError(1, "email", "bad", "INVALID_EMAIL", "Bad email")));
        importJobRepository.save(job);

        Instant discardedAt = createdAt.plusSeconds(15);
        job.discard(discardedAt);
        importJobRepository.save(job);

        ImportJob reloaded = importJobRepository.findById(jobId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ImportStatus.DISCARDED);
        assertThat(reloaded.getDiscardedAt()).isEqualTo(discardedAt);
        assertThat(reloaded.isTerminal()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("IJ-05: Cascading deletion removes import job and all associated error records")
    void shouldCascadeDeleteErrorsWhenJobDeleted() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId,
            ImportType.STUDENTS,
            ImportMode.PARTIAL_COMMIT,
            "to_delete.xlsx",
            testUserId,
            Instant.now()
        );
        job.stage(10, 8, List.of(
            new RowValidationError(1, "c1", "v1", "ERR1", "Error 1"),
            new RowValidationError(2, "c2", "v2", "ERR2", "Error 2")
        ));
        importJobRepository.save(job);

        List<ImportJobErrorEntity> errorsBefore = errorRepository.findByJobIdOrderByRowIndexAsc(jobId);
        assertThat(errorsBefore).hasSize(2);

        importJobRepository.deleteById(jobId);

        assertThat(importJobRepository.findById(jobId)).isEmpty();
        List<ImportJobErrorEntity> errorsAfter = errorRepository.findByJobIdOrderByRowIndexAsc(jobId);
        assertThat(errorsAfter).isEmpty();
    }

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("IJ-06: Reject ImportJob creation with non-existent user (Foreign Key Integrity)")
    void shouldRejectJobWithInvalidUserForeignKey() {
        UUID nonExistentUserId = UUID.randomUUID();
        ImportJob invalidJob = ImportJob.create(
            UUID.randomUUID(),
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "unauthorized.xlsx",
            nonExistentUserId,
            Instant.now()
        );
        invalidJob.stage(5, 5, List.of());

        assertThatThrownBy(() -> {
            importJobRepository.save(invalidJob);
            entityManager.flush();
        }).isInstanceOfAny(
            DataIntegrityViolationException.class,
            jakarta.persistence.PersistenceException.class,
            org.hibernate.exception.ConstraintViolationException.class
        );
    }
}
