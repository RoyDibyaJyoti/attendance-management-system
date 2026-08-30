package com.amcs.application.service;

import com.amcs.application.exception.AccessDeniedException;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ExcelParsingPort;
import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.port.out.importer.ImportStagedRow;
import com.amcs.application.port.out.importer.ImportStagedRowRepositoryPort;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.application.service.importer.payload.StagedStudentPayload;
import com.amcs.application.service.importer.validator.AttendanceRowValidator;
import com.amcs.application.service.importer.validator.SessionRowValidator;
import com.amcs.application.service.importer.validator.StudentRowValidator;
import com.amcs.application.service.importer.validator.ValidationOutcome;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportApplicationService Unit Tests")
class ImportApplicationServiceTest {

    @Mock private ExcelParsingPort excelParsingPort;
    @Mock private ExcelWorkbookGeneratorPort workbookGeneratorPort;
    @Mock private ImportJobRepositoryPort importJobRepository;
    @Mock private ImportStagedRowRepositoryPort stagedRowRepository;
    @Mock private ApplicationAuthorizationService authorizationService;
    @Mock private CurrentUserPort currentUserPort;

    @Mock private StudentRowValidator studentValidator;
    @Mock private SessionRowValidator sessionValidator;
    @Mock private AttendanceRowValidator attendanceValidator;

    @Mock private StudentRepositoryPort studentRepository;
    @Mock private EnrollmentRepositoryPort enrollmentRepository;
    @Mock private SessionRepositoryPort sessionRepository;
    @Mock private AttendanceRecordRepositoryPort attendanceRecordRepository;

    private ObjectMapper objectMapper;
    private ImportApplicationService service;

    private final UUID adminUserId = UUID.randomUUID();
    private final AuthenticatedActor adminActor = new AuthenticatedActor(
        adminUserId, "admin", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty()
    );

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ImportApplicationService(
            excelParsingPort,
            workbookGeneratorPort,
            importJobRepository,
            stagedRowRepository,
            authorizationService,
            currentUserPort,
            studentValidator,
            sessionValidator,
            attendanceValidator,
            studentRepository,
            enrollmentRepository,
            sessionRepository,
            attendanceRecordRepository,
            objectMapper
        );
    }

    @Test
    @DisplayName("Clean student import with FAIL_FAST stages successfully")
    void shouldStageCleanStudentImportFailFast() {
        when(currentUserPort.requireCurrentActor()).thenReturn(adminActor);
        when(excelParsingPort.validateHeaders(any(), anyList(), anyList()))
            .thenReturn(HeaderValidationResult.valid(Map.of("Registration Number", 0)));

        ParsedRow header = new ParsedRow(1, Map.of(), List.of(), false);
        ParsedRow row1 = new ParsedRow(2, Map.of("Registration Number", "CS01"), List.of("CS01"), false);
        ParsedRow row2 = new ParsedRow(3, Map.of("Registration Number", "CS02"), List.of("CS02"), false);

        doAnswer(invocation -> {
            Consumer<List<ParsedRow>> consumer = invocation.getArgument(2);
            consumer.accept(List.of(header, row1, row2));
            return null;
        }).when(excelParsingPort).parseStreaming(any(InputStream.class), anyInt(), any());

        StagedStudentPayload payload1 = new StagedStudentPayload("CS01", "Alice", "alice@test.com", UUID.randomUUID(), UUID.randomUUID());
        StagedStudentPayload payload2 = new StagedStudentPayload("CS02", "Bob", "bob@test.com", UUID.randomUUID(), UUID.randomUUID());

        when(studentValidator.validateRow(eq(row1), any(), any())).thenReturn(ValidationOutcome.valid(payload1));
        when(studentValidator.validateRow(eq(row2), any(), any())).thenReturn(ValidationOutcome.valid(payload2));

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportSubmissionResult result = service.submitImport(
            new ByteArrayInputStream(new byte[]{1, 2, 3}),
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "students.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.STAGED_CLEAN);
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.invalidRows()).isEqualTo(0);
        assertThat(result.isCommittable()).isTrue();
        verify(stagedRowRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Student import with errors under FAIL_FAST rejects job and purges staged rows")
    void shouldRejectImportUnderFailFastWhenErrorsEncountered() {
        when(currentUserPort.requireCurrentActor()).thenReturn(adminActor);
        when(excelParsingPort.validateHeaders(any(), anyList(), anyList()))
            .thenReturn(HeaderValidationResult.valid(Map.of("Registration Number", 0)));

        ParsedRow header = new ParsedRow(1, Map.of(), List.of(), false);
        ParsedRow row1 = new ParsedRow(2, Map.of("Registration Number", "CS01"), List.of("CS01"), false);
        ParsedRow row2 = new ParsedRow(3, Map.of("Registration Number", "INVALID"), List.of("INVALID"), false);

        doAnswer(invocation -> {
            Consumer<List<ParsedRow>> consumer = invocation.getArgument(2);
            consumer.accept(List.of(header, row1, row2));
            return null;
        }).when(excelParsingPort).parseStreaming(any(InputStream.class), anyInt(), any());

        StagedStudentPayload payload1 = new StagedStudentPayload("CS01", "Alice", "alice@test.com", UUID.randomUUID(), UUID.randomUUID());
        RowValidationError error2 = new RowValidationError(3, "Registration Number", "INVALID", "INVALID_FORMAT", "Invalid format");

        when(studentValidator.validateRow(eq(row1), any(), any())).thenReturn(ValidationOutcome.valid(payload1));
        when(studentValidator.validateRow(eq(row2), any(), any())).thenReturn(ValidationOutcome.invalid(error2));

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportSubmissionResult result = service.submitImport(
            new ByteArrayInputStream(new byte[]{1, 2, 3}),
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "students.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.REJECTED);
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(1);
        assertThat(result.invalidRows()).isEqualTo(1);
        assertThat(result.isCommittable()).isFalse();
        verify(stagedRowRepository).deleteByJobId(result.jobId());
    }

    @Test
    @DisplayName("Student import with errors under PARTIAL_COMMIT stages clean rows and isolates errors")
    void shouldStagePartialWhenErrorsEncounteredUnderPartialCommit() {
        when(currentUserPort.requireCurrentActor()).thenReturn(adminActor);
        when(excelParsingPort.validateHeaders(any(), anyList(), anyList()))
            .thenReturn(HeaderValidationResult.valid(Map.of("Registration Number", 0)));

        ParsedRow header = new ParsedRow(1, Map.of(), List.of(), false);
        ParsedRow row1 = new ParsedRow(2, Map.of("Registration Number", "CS01"), List.of("CS01"), false);
        ParsedRow row2 = new ParsedRow(3, Map.of("Registration Number", "INVALID"), List.of("INVALID"), false);

        doAnswer(invocation -> {
            Consumer<List<ParsedRow>> consumer = invocation.getArgument(2);
            consumer.accept(List.of(header, row1, row2));
            return null;
        }).when(excelParsingPort).parseStreaming(any(InputStream.class), anyInt(), any());

        StagedStudentPayload payload1 = new StagedStudentPayload("CS01", "Alice", "alice@test.com", UUID.randomUUID(), UUID.randomUUID());
        RowValidationError error2 = new RowValidationError(3, "Registration Number", "INVALID", "INVALID_FORMAT", "Invalid format");

        when(studentValidator.validateRow(eq(row1), any(), any())).thenReturn(ValidationOutcome.valid(payload1));
        when(studentValidator.validateRow(eq(row2), any(), any())).thenReturn(ValidationOutcome.invalid(error2));

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportSubmissionResult result = service.submitImport(
            new ByteArrayInputStream(new byte[]{1, 2, 3}),
            ImportType.STUDENTS,
            ImportMode.PARTIAL_COMMIT,
            "students.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(1);
        assertThat(result.invalidRows()).isEqualTo(1);
        assertThat(result.isCommittable()).isTrue();
        verify(stagedRowRepository, never()).deleteByJobId(any());
    }

    @Test
    @DisplayName("Commit staged import saves entities and marks job COMMITTED")
    void shouldCommitStagedImport() throws Exception {
        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "students.xlsx", adminUserId, Instant.now()
        );
        job.stage(1, 1, List.of()); // STAGED_CLEAN

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        UUID deptId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        StagedStudentPayload payload = new StagedStudentPayload("CS01", "Alice", "alice@univ.edu", deptId, sectionId);
        String json = objectMapper.writeValueAsString(payload);
        ImportStagedRow stagedRow = new ImportStagedRow(UUID.randomUUID(), jobId, 2, "STUDENTS", json);

        when(stagedRowRepository.findByJobId(jobId)).thenReturn(List.of(stagedRow));

        ImportCommitResult commitResult = service.commitImport(jobId);

        assertThat(commitResult.status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(commitResult.committedRows()).isEqualTo(1);
        verify(studentRepository).save(any());
        verify(enrollmentRepository).save(any());
        verify(stagedRowRepository).deleteByJobId(jobId);
    }

    @Test
    @DisplayName("Discard staged import marks job DISCARDED and purges staged rows")
    void shouldDiscardStagedImport() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "students.xlsx", adminUserId, Instant.now()
        );
        job.stage(2, 1, List.of(new RowValidationError(3, "A", "B", "ERR", "Err"))); // STAGED_PARTIAL

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportSubmissionResult result = service.discardImport(jobId);

        assertThat(result.status()).isEqualTo(ImportStatus.DISCARDED);
        verify(stagedRowRepository).deleteByJobId(jobId);
    }

    @Test
    @DisplayName("Faculty attempting student roster import throws AccessDeniedException")
    void shouldDenyStudentImportForFaculty() {
        doThrow(new AccessDeniedException("Access denied: Only administrators may import student rosters"))
            .when(authorizationService).requireImportSubmissionAccess(ImportType.STUDENTS);

        assertThatThrownBy(() -> service.submitImport(
            new ByteArrayInputStream(new byte[]{1, 2}),
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "students.xlsx"
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Cross-user commit throws AccessDeniedException (IDOR)")
    void shouldPreventCrossUserCommit() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId, ImportType.SESSIONS, ImportMode.FAIL_FAST, "sessions.xlsx", UUID.randomUUID(), Instant.now()
        );
        job.stage(1, 1, List.of());

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        doThrow(new AccessDeniedException("Access denied: You cannot commit an import job submitted by another user"))
            .when(authorizationService).requireImportJobManageAccess(job, "commit");

        assertThatThrownBy(() -> service.commitImport(jobId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Non-existent job throws ResourceNotFoundException")
    void shouldThrowResourceNotFoundWhenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(importJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getJob(jobId))
            .isInstanceOf(com.amcs.application.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Download error report delegates to ExcelWorkbookGeneratorPort")
    void shouldDownloadErrorReport() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId, ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "students.xlsx", adminUserId, Instant.now()
        );
        RowValidationError err = new RowValidationError(2, "Email", "bad", "INVALID_EMAIL", "Malformed");
        job.stage(2, 1, List.of(err));

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(workbookGeneratorPort.generateErrorReport(job.getErrors())).thenReturn(new byte[]{1, 2, 3});

        byte[] report = service.getJobErrorReport(jobId);
        assertThat(report).isEqualTo(new byte[]{1, 2, 3});
        verify(authorizationService).requireImportJobManageAccess(job, "download error report");
    }
}
