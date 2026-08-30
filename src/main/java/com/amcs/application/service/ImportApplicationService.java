package com.amcs.application.service;

import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ExcelParsingPort;
import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.application.port.out.excel.ImportSchemaDefinition;
import com.amcs.application.port.out.importer.ImportStagedRow;
import com.amcs.application.port.out.importer.ImportStagedRowRepositoryPort;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.application.service.importer.payload.StagedAttendancePayload;
import com.amcs.application.service.importer.payload.StagedSessionPayload;
import com.amcs.application.service.importer.payload.StagedStudentPayload;
import com.amcs.application.service.importer.validator.AttendanceRowValidator;
import com.amcs.application.service.importer.validator.SessionRowValidator;
import com.amcs.application.service.importer.validator.StudentRowValidator;
import com.amcs.application.service.importer.validator.ValidationOutcome;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Primary application service orchestrating bulk entity import pipelines.
 *
 * <p>Coordinates streaming XLSX ingestion, multi-level validation, durable staging,
 * and transactional batch commit/discard workflows while enforcing strict RBAC and IDOR policies.
 */
@Service
public class ImportApplicationService {

    private final ExcelParsingPort excelParsingPort;
    private final ExcelWorkbookGeneratorPort workbookGeneratorPort;
    private final ImportJobRepositoryPort importJobRepository;
    private final ImportStagedRowRepositoryPort stagedRowRepository;
    private final ApplicationAuthorizationService authorizationService;
    private final CurrentUserPort currentUserPort;

    private final StudentRowValidator studentValidator;
    private final SessionRowValidator sessionValidator;
    private final AttendanceRowValidator attendanceValidator;

    private final StudentRepositoryPort studentRepository;
    private final EnrollmentRepositoryPort enrollmentRepository;
    private final SessionRepositoryPort sessionRepository;
    private final AttendanceRecordRepositoryPort attendanceRecordRepository;

    private final ObjectMapper objectMapper;

    public ImportApplicationService(
        ExcelParsingPort excelParsingPort,
        ExcelWorkbookGeneratorPort workbookGeneratorPort,
        ImportJobRepositoryPort importJobRepository,
        ImportStagedRowRepositoryPort stagedRowRepository,
        ApplicationAuthorizationService authorizationService,
        CurrentUserPort currentUserPort,
        StudentRowValidator studentValidator,
        SessionRowValidator sessionValidator,
        AttendanceRowValidator attendanceValidator,
        StudentRepositoryPort studentRepository,
        EnrollmentRepositoryPort enrollmentRepository,
        SessionRepositoryPort sessionRepository,
        AttendanceRecordRepositoryPort attendanceRecordRepository,
        ObjectMapper objectMapper
    ) {
        this.excelParsingPort = Objects.requireNonNull(excelParsingPort, "excelParsingPort");
        this.workbookGeneratorPort = Objects.requireNonNull(workbookGeneratorPort, "workbookGeneratorPort");
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository");
        this.stagedRowRepository = Objects.requireNonNull(stagedRowRepository, "stagedRowRepository");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
        this.currentUserPort = Objects.requireNonNull(currentUserPort, "currentUserPort");
        this.studentValidator = Objects.requireNonNull(studentValidator, "studentValidator");
        this.sessionValidator = Objects.requireNonNull(sessionValidator, "sessionValidator");
        this.attendanceValidator = Objects.requireNonNull(attendanceValidator, "attendanceValidator");
        this.studentRepository = Objects.requireNonNull(studentRepository, "studentRepository");
        this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository, "enrollmentRepository");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.attendanceRecordRepository = Objects.requireNonNull(attendanceRecordRepository, "attendanceRecordRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Ingests, inspects, validates, and stages an uploaded spreadsheet.
     */
    public ImportSubmissionResult submitImport(
        InputStream inputStream,
        ImportType importType,
        ImportMode importMode,
        String originalFilename
    ) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(importType, "importType must not be null");
        Objects.requireNonNull(importMode, "importMode must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");

        authorizationService.requireImportSubmissionAccess(importType);
        AuthenticatedActor actor = currentUserPort.requireCurrentActor();

        UUID jobId = UUID.randomUUID();
        ImportJob job = ImportJob.create(
            jobId,
            importType,
            importMode,
            originalFilename,
            actor.userId(),
            Instant.now()
        );
        importJobRepository.save(job);

        byte[] fileBytes;
        try {
            fileBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input spreadsheet: " + e.getMessage(), e);
        }

        // 1. Header Validation
        List<String> requiredHeaders = ImportSchemaDefinition.getRequiredHeaders(importType);
        List<String> optionalHeaders = ImportSchemaDefinition.getOptionalHeaders(importType);

        HeaderValidationResult headerResult = excelParsingPort.validateHeaders(
            new ByteArrayInputStream(fileBytes), requiredHeaders, optionalHeaders
        );

        if (!headerResult.isValid()) {
            job.stage(0, 0, headerResult.errors());
            ImportJob saved = importJobRepository.save(job);
            return ImportSubmissionResult.from(saved);
        }

        // 2. Memory-bounded streaming validation & staging
        Set<String> seenRegNos = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenSessions = new HashSet<>();
        Set<String> seenAttendance = new HashSet<>();

        int[] metrics = new int[2]; // metrics[0] = total, metrics[1] = valid
        List<RowValidationError> collectedErrors = new ArrayList<>();
        List<ImportStagedRow> stagedChunk = new ArrayList<>();

        excelParsingPort.parseStreaming(new ByteArrayInputStream(fileBytes), 250, batch -> {
            for (var row : batch) {
                if (row.rowIndex() == 1 || row.isEmpty()) {
                    continue; // Skip header row and empty rows
                }

                metrics[0]++; // Count data row
                ValidationOutcome<?> outcome = switch (importType) {
                    case STUDENTS -> studentValidator.validateRow(row, seenRegNos, seenEmails);
                    case SESSIONS -> sessionValidator.validateRow(row, seenSessions, actor);
                    case ATTENDANCE_RECORDS -> attendanceValidator.validateRow(row, seenAttendance, actor);
                };

                if (outcome.isValid()) {
                    metrics[1]++;
                    try {
                        String json = objectMapper.writeValueAsString(outcome.getPayload().orElseThrow());
                        stagedChunk.add(ImportStagedRow.create(jobId, row.rowIndex(), importType.name(), json));
                    } catch (Exception e) {
                        collectedErrors.add(new RowValidationError(
                            row.rowIndex(), "Row", "", "SERIALIZATION_ERROR", "Failed to stage row: " + e.getMessage()
                        ));
                    }
                } else {
                    collectedErrors.add(outcome.getError().orElseThrow());
                }

                if (stagedChunk.size() >= 250) {
                    stagedRowRepository.saveAll(stagedChunk);
                    stagedChunk.clear();
                }
            }
        });

        if (!stagedChunk.isEmpty()) {
            stagedRowRepository.saveAll(stagedChunk);
            stagedChunk.clear();
        }

        // 3. Apply Import Mode Rules
        if (importMode == ImportMode.FAIL_FAST && !collectedErrors.isEmpty()) {
            stagedRowRepository.deleteByJobId(jobId); // Purge staged rows under fail-fast
            job.stage(metrics[0], metrics[1], collectedErrors); // Transitions to REJECTED
        } else {
            job.stage(metrics[0], metrics[1], collectedErrors); // Clean or Partial
        }

        ImportJob saved = importJobRepository.save(job);
        return ImportSubmissionResult.from(saved);
    }

    /**
     * Atomically executes the commit of all staged valid rows for an import job.
     */
    @Transactional
    public ImportCommitResult commitImport(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        authorizationService.requireImportJobManageAccess(job, "commit");

        if (!job.isCommittable()) {
            throw new IllegalStateException("Import job cannot be committed in status: " + job.getStatus());
        }

        List<ImportStagedRow> stagedRows = stagedRowRepository.findByJobId(jobId);
        if (stagedRows.isEmpty()) {
            throw new IllegalStateException("Cannot commit job with zero staged rows");
        }

        executeCommit(job.getImportType(), stagedRows);

        Instant committedAt = Instant.now();
        job.commit(committedAt);
        importJobRepository.save(job);
        stagedRowRepository.deleteByJobId(jobId);

        return new ImportCommitResult(job.getId(), job.getStatus(), stagedRows.size(), committedAt);
    }

    /**
     * Cancels and discards an import job, purging all staged rows.
     */
    @Transactional
    public ImportSubmissionResult discardImport(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        authorizationService.requireImportJobManageAccess(job, "discard");

        Instant discardedAt = Instant.now();
        job.discard(discardedAt);
        stagedRowRepository.deleteByJobId(jobId);

        ImportJob saved = importJobRepository.save(job);
        return ImportSubmissionResult.from(saved);
    }

    /**
     * Retrieves status and diagnostics for an existing import job.
     */
    @Transactional(readOnly = true)
    public ImportSubmissionResult getJob(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        authorizationService.requireImportJobManageAccess(job, "view");
        return ImportSubmissionResult.from(job);
    }

    /**
     * Generates a streaming XLSX error report summarizing validation errors for an import job.
     */
    @Transactional(readOnly = true)
    public byte[] getJobErrorReport(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        ImportJob job = importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        authorizationService.requireImportJobManageAccess(job, "download error report");
        return workbookGeneratorPort.generateErrorReport(job.getErrors());
    }

    private void executeCommit(ImportType importType, List<ImportStagedRow> stagedRows) {
        for (ImportStagedRow row : stagedRows) {
            try {
                switch (importType) {
                    case STUDENTS -> {
                        StagedStudentPayload payload = objectMapper.readValue(
                            row.payloadJson(), StagedStudentPayload.class
                        );
                        StudentEntity student = new StudentEntity(
                            UUID.randomUUID(),
                            payload.registrationNumber(),
                            payload.name(),
                            payload.email(),
                            payload.departmentId()
                        );
                        studentRepository.save(student);

                        Enrollment enrollment = new Enrollment(
                            student.getId(),
                            payload.sectionId(),
                            LocalDate.now(),
                            java.util.Optional.empty(),
                            java.util.Optional.empty()
                        );
                        enrollmentRepository.save(enrollment);
                    }
                    case SESSIONS -> {
                        StagedSessionPayload payload = objectMapper.readValue(
                            row.payloadJson(), StagedSessionPayload.class
                        );
                        Session session = new Session(
                            UUID.randomUUID(),
                            payload.subjectId(),
                            payload.sectionId(),
                            payload.facultyId(),
                            payload.sessionDate(),
                            payload.sessionType(),
                            payload.plannedUnits(),
                            0,
                            com.amcs.domain.attendance.SessionStatus.SCHEDULED,
                            java.util.Optional.ofNullable(payload.labGroupId()),
                            java.util.Optional.empty()
                        );
                        sessionRepository.save(session, payload.academicPeriodId());
                    }
                    case ATTENDANCE_RECORDS -> {
                        StagedAttendancePayload payload = objectMapper.readValue(
                            row.payloadJson(), StagedAttendancePayload.class
                        );
                        AttendanceRecord record = new AttendanceRecord(
                            UUID.randomUUID(),
                            payload.sessionId(),
                            payload.studentId(),
                            payload.status()
                        );
                        attendanceRecordRepository.save(record);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to commit staged row " + row.rowIndex() + ": " + e.getMessage(), e);
            }
        }
    }
}
