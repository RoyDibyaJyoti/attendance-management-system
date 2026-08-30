package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.importer.ImportCommitResponse;
import com.amcs.application.dto.importer.ImportJobResponse;
import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.security.ApplicationAuthorizationService;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.excel.security.XlsxSecurityInspector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Bulk Import Pipeline", description = "Endpoints for streaming XLSX ingestion, staging, error reporting, and two-phase commit")
public class ImportController {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportApplicationService importApplicationService;
    private final ExcelWorkbookGeneratorPort workbookGeneratorPort;
    private final ApplicationAuthorizationService authorizationService;
    private final XlsxSecurityInspector securityInspector;

    public ImportController(
        ImportApplicationService importApplicationService,
        ExcelWorkbookGeneratorPort workbookGeneratorPort,
        ApplicationAuthorizationService authorizationService
    ) {
        this.importApplicationService = Objects.requireNonNull(importApplicationService, "importApplicationService");
        this.workbookGeneratorPort = Objects.requireNonNull(workbookGeneratorPort, "workbookGeneratorPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
        this.securityInspector = new XlsxSecurityInspector();
    }

    @GetMapping("/templates/{type}")
    @Operation(summary = "Download XLSX import template", description = "Generates a standard XLSX template with headers and cell validation dropdowns")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template XLSX file", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "400", description = "Unsupported import type"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied (Students cannot download templates)")
    })
    public ResponseEntity<byte[]> downloadTemplate(
        @Parameter(description = "Import category (STUDENTS, SESSIONS, ATTENDANCE_RECORDS)")
        @PathVariable("type") String type
    ) {
        ImportType importType = parseImportType(type);
        authorizationService.requireFacultyOrAdmin("download import templates");

        byte[] templateBytes = workbookGeneratorPort.generateTemplate(importType);
        String filename = "template-" + importType.name().toLowerCase() + ".xlsx";

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(templateBytes);
    }

    @PostMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and stage spreadsheet", description = "Inspects XLSX security, validates rows, and stages committable entities")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Spreadsheet successfully parsed and staged", content = @Content(schema = @Schema(implementation = ImportJobResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing file, empty file, or invalid import mode"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden (Actor role unauthorized for this import type)"),
        @ApiResponse(responseCode = "422", description = "Malformed spreadsheet, ZIP bomb, or security violation")
    })
    public ResponseEntity<ImportJobResponse> uploadImport(
        @Parameter(description = "Import category (STUDENTS, SESSIONS, ATTENDANCE_RECORDS)")
        @PathVariable("type") String type,
        @Parameter(description = "XLSX file to upload")
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "Import mode (FAIL_FAST or PARTIAL_COMMIT)")
        @RequestParam(value = "mode", defaultValue = "FAIL_FAST") String mode
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Spreadsheet file is required and cannot be empty");
        }

        ImportType importType = parseImportType(type);
        ImportMode importMode = parseImportMode(mode);

        byte[] fileBytes = file.getBytes();
        // Early security inspection for magic bytes, compression limits, path traversal
        securityInspector.inspect(fileBytes);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : (type + ".xlsx");
        ImportSubmissionResult result = importApplicationService.submitImport(
            file.getInputStream(),
            importType,
            importMode,
            originalFilename
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ImportJobResponse.from(result));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get import job status", description = "Returns job status, metrics, committability, and itemized validation diagnostics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import job status details", content = @Content(schema = @Schema(implementation = ImportJobResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden (IDOR protection: user is not the job submitter nor an admin)"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<ImportJobResponse> getJob(
        @Parameter(description = "Import job identifier")
        @PathVariable("jobId") UUID jobId
    ) {
        ImportSubmissionResult result = importApplicationService.getJob(jobId);
        return ResponseEntity.ok(ImportJobResponse.from(result));
    }

    @GetMapping("/jobs/{jobId}/errors/download")
    @Operation(summary = "Download error report XLSX", description = "Generates and streams an XLSX workbook summarizing all row validation failures")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Error report XLSX file", content = @Content(mediaType = XLSX_MEDIA_TYPE)),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden (IDOR protection: user cannot access another user's error report)"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<byte[]> downloadErrorReport(
        @Parameter(description = "Import job identifier")
        @PathVariable("jobId") UUID jobId
    ) {
        byte[] errorReportBytes = importApplicationService.getJobErrorReport(jobId);
        String filename = "errors-" + jobId + ".xlsx";

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(errorReportBytes);
    }

    @PostMapping("/jobs/{jobId}/commit")
    @Operation(summary = "Commit staged import", description = "Atomically executes and commits all staged valid rows to the primary database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import successfully committed", content = @Content(schema = @Schema(implementation = ImportCommitResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden (IDOR protection: user cannot commit another user's job)"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "409", description = "Lifecycle conflict (job is not in a committable state or already committed/discarded)")
    })
    public ResponseEntity<ImportCommitResponse> commitJob(
        @Parameter(description = "Import job identifier")
        @PathVariable("jobId") UUID jobId
    ) {
        ImportCommitResult result = importApplicationService.commitImport(jobId);
        return ResponseEntity.ok(ImportCommitResponse.from(result));
    }

    @PostMapping("/jobs/{jobId}/discard")
    @Operation(summary = "Discard staged import", description = "Cancels a staged import job and purges all staged rows from database")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Import job successfully discarded"),
        @ApiResponse(responseCode = "401", description = "Unauthenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden (IDOR protection: user cannot discard another user's job)"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "409", description = "Lifecycle conflict (cannot discard an already committed job)")
    })
    public ResponseEntity<Void> discardJob(
        @Parameter(description = "Import job identifier")
        @PathVariable("jobId") UUID jobId
    ) {
        importApplicationService.discardImport(jobId);
        return ResponseEntity.noContent().build();
    }

    private ImportType parseImportType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Import type is required");
        }
        try {
            return ImportType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid import type: '" + raw + "'. Supported: STUDENTS, SESSIONS, ATTENDANCE_RECORDS");
        }
    }

    private ImportMode parseImportMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ImportMode.FAIL_FAST;
        }
        try {
            return ImportMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid import mode: '" + raw + "'. Supported: FAIL_FAST, PARTIAL_COMMIT");
        }
    }
}
