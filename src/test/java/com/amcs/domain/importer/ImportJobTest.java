package com.amcs.domain.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportJob Domain Model & Lifecycle Tests")
class ImportJobTest {

    private final UUID jobId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-30T10:00:00Z");

    @Nested
    @DisplayName("Creation & Invariant Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create newly submitted import job with zero counts")
        void shouldCreateInitialJob() {
            ImportJob job = ImportJob.create(
                jobId,
                ImportType.STUDENTS,
                ImportMode.FAIL_FAST,
                "students_2026.xlsx",
                userId,
                now
            );

            assertThat(job.getId()).isEqualTo(jobId);
            assertThat(job.getImportType()).isEqualTo(ImportType.STUDENTS);
            assertThat(job.getImportMode()).isEqualTo(ImportMode.FAIL_FAST);
            assertThat(job.getStatus()).isEqualTo(ImportStatus.SUBMITTED);
            assertThat(job.getOriginalFilename()).isEqualTo("students_2026.xlsx");
            assertThat(job.getTotalRows()).isZero();
            assertThat(job.getValidRows()).isZero();
            assertThat(job.getInvalidRows()).isZero();
            assertThat(job.getErrors()).isEmpty();
            assertThat(job.getCreatedByUserId()).isEqualTo(userId);
            assertThat(job.getCreatedAt()).isEqualTo(now);
            assertThat(job.getCommittedAt()).isNull();
            assertThat(job.getDiscardedAt()).isNull();
            assertThat(job.isCommittable()).isFalse();
            assertThat(job.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("Should reject blank filename or negative row counts")
        void shouldRejectInvalidArguments() {
            assertThatThrownBy(() -> ImportJob.create(jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, "", userId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("originalFilename must not be blank");

            assertThatThrownBy(() -> ImportJob.reconstitute(jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, ImportStatus.SUBMITTED,
                "file.xlsx", -1, 0, 0, List.of(), userId, now, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Row counts cannot be negative");
        }
    }

    @Nested
    @DisplayName("Staging Lifecycle Tests")
    class StagingTests {

        @Test
        @DisplayName("Clean parse transitions to STAGED_CLEAN")
        void shouldTransitionToStagedCleanWhenNoErrors() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);

            job.stage(100, 100, List.of());

            assertThat(job.getStatus()).isEqualTo(ImportStatus.STAGED_CLEAN);
            assertThat(job.getTotalRows()).isEqualTo(100);
            assertThat(job.getValidRows()).isEqualTo(100);
            assertThat(job.getInvalidRows()).isZero();
            assertThat(job.hasErrors()).isFalse();
            assertThat(job.isCommittable()).isTrue();
        }

        @Test
        @DisplayName("Errors under FAIL_FAST mode transition directly to REJECTED")
        void shouldTransitionToRejectedWhenErrorsInFailFast() {
            ImportJob job = ImportJob.create(jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, "sessions.xlsx", userId, now);

            RowValidationError error = new RowValidationError(2, "facultyId", "unknown-emp", "INVALID_FACULTY", "Faculty not found");
            job.stage(50, 49, List.of(error));

            assertThat(job.getStatus()).isEqualTo(ImportStatus.REJECTED);
            assertThat(job.getTotalRows()).isEqualTo(50);
            assertThat(job.getValidRows()).isEqualTo(49);
            assertThat(job.getInvalidRows()).isEqualTo(1);
            assertThat(job.hasErrors()).isTrue();
            assertThat(job.isCommittable()).isFalse();
            assertThat(job.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Errors under PARTIAL_COMMIT mode transition to STAGED_PARTIAL")
        void shouldTransitionToStagedPartialWhenErrorsInPartialCommit() {
            ImportJob job = ImportJob.create(jobId, ImportType.ATTENDANCE_RECORDS, ImportMode.PARTIAL_COMMIT, "attendance.xlsx", userId, now);

            RowValidationError err1 = new RowValidationError(5, "status", "INVALID", "INVALID_STATUS", "Invalid status value");
            RowValidationError err2 = new RowValidationError(12, "studentId", "none", "STUDENT_NOT_FOUND", "Student not found");

            job.stage(100, 98, List.of(err1, err2));

            assertThat(job.getStatus()).isEqualTo(ImportStatus.STAGED_PARTIAL);
            assertThat(job.getTotalRows()).isEqualTo(100);
            assertThat(job.getValidRows()).isEqualTo(98);
            assertThat(job.getInvalidRows()).isEqualTo(2);
            assertThat(job.hasErrors()).isTrue();
            assertThat(job.isCommittable()).isTrue();
            assertThat(job.getErrors()).hasSize(2);
        }

        @Test
        @DisplayName("Zero valid rows transitions to REJECTED even under PARTIAL_COMMIT mode")
        void shouldRejectWhenZeroValidRows() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "file.xlsx", userId, now);

            RowValidationError err = new RowValidationError(1, "email", "bad", "INVALID_EMAIL", "Invalid email format");
            job.stage(1, 0, List.of(err));

            assertThat(job.getStatus()).isEqualTo(ImportStatus.REJECTED);
            assertThat(job.isCommittable()).isFalse();
        }

        @Test
        @DisplayName("Cannot stage a job twice or when not in SUBMITTED state")
        void shouldRejectStagingWhenNotSubmitted() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            job.stage(10, 10, List.of());

            assertThatThrownBy(() -> job.stage(10, 10, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only jobs in SUBMITTED state can be staged");
        }
    }

    @Nested
    @DisplayName("Commit Lifecycle Tests")
    class CommitTests {

        @Test
        @DisplayName("Should successfully commit STAGED_CLEAN job")
        void shouldCommitStagedCleanJob() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            job.stage(10, 10, List.of());

            Instant commitTime = now.plusSeconds(60);
            job.commit(commitTime);

            assertThat(job.getStatus()).isEqualTo(ImportStatus.COMMITTED);
            assertThat(job.getCommittedAt()).isEqualTo(commitTime);
            assertThat(job.isTerminal()).isTrue();
            assertThat(job.isCommittable()).isFalse();
        }

        @Test
        @DisplayName("Should successfully commit STAGED_PARTIAL job")
        void shouldCommitStagedPartialJob() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "file.xlsx", userId, now);
            RowValidationError err = new RowValidationError(3, "name", "", "REQUIRED_FIELD", "Name is required");
            job.stage(10, 9, List.of(err));

            Instant commitTime = now.plusSeconds(30);
            job.commit(commitTime);

            assertThat(job.getStatus()).isEqualTo(ImportStatus.COMMITTED);
            assertThat(job.getCommittedAt()).isEqualTo(commitTime);
        }

        @Test
        @DisplayName("Cannot commit un-staged, rejected, or discarded jobs")
        void shouldRejectInvalidCommitTransitions() {
            ImportJob submittedJob = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            assertThatThrownBy(() -> submittedJob.commit(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot commit an import job before it has been staged");

            ImportJob rejectedJob = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            rejectedJob.stage(10, 8, List.of(new RowValidationError(1, "c", "v", "CODE", "msg")));
            assertThatThrownBy(() -> rejectedJob.commit(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot commit a rejected import job");
        }

        @Test
        @DisplayName("Cannot commit already committed job")
        void shouldPreventDoubleCommit() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            job.stage(5, 5, List.of());
            job.commit(now);

            assertThatThrownBy(() -> job.commit(now.plusSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Import job has already been committed");
        }
    }

    @Nested
    @DisplayName("Discard Lifecycle Tests")
    class DiscardTests {

        @Test
        @DisplayName("Should discard staged job")
        void shouldDiscardStagedJob() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "file.xlsx", userId, now);
            job.stage(10, 8, List.of(new RowValidationError(1, "c", "v", "CODE", "msg")));

            Instant discardTime = now.plusSeconds(15);
            job.discard(discardTime);

            assertThat(job.getStatus()).isEqualTo(ImportStatus.DISCARDED);
            assertThat(job.getDiscardedAt()).isEqualTo(discardTime);
            assertThat(job.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Cannot discard an already committed job")
        void shouldPreventDiscardingCommittedJob() {
            ImportJob job = ImportJob.create(jobId, ImportType.STUDENTS, ImportMode.FAIL_FAST, "file.xlsx", userId, now);
            job.stage(5, 5, List.of());
            job.commit(now);

            assertThatThrownBy(() -> job.discard(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot discard an already committed import job");
        }
    }

    @Nested
    @DisplayName("RowValidationError Validation Tests")
    class RowValidationErrorTests {

        @Test
        @DisplayName("Valid RowValidationError created successfully")
        void shouldCreateValidRowValidationError() {
            RowValidationError error = new RowValidationError(5, "email", "invalid-email", "INVALID_FORMAT", "Email is malformed");
            assertThat(error.rowIndex()).isEqualTo(5);
            assertThat(error.columnName()).isEqualTo("email");
            assertThat(error.rejectedValue()).isEqualTo("invalid-email");
            assertThat(error.errorCode()).isEqualTo("INVALID_FORMAT");
            assertThat(error.errorMessage()).isEqualTo("Email is malformed");
        }

        @Test
        @DisplayName("Row index < 1 is rejected")
        void shouldRejectInvalidRowIndex() {
            assertThatThrownBy(() -> new RowValidationError(0, "email", "val", "CODE", "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Row index must be >= 1");
        }

        @Test
        @DisplayName("Blank errorCode or errorMessage is rejected")
        void shouldRejectBlankCodes() {
            assertThatThrownBy(() -> new RowValidationError(1, "email", "val", "  ", "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("errorCode must not be blank");

            assertThatThrownBy(() -> new RowValidationError(1, "email", "val", "CODE", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("errorMessage must not be blank");
        }
    }
}
