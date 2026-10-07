package com.amcs.importer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/import")
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleImportController {

    private final ScheduleImportService importService;

    public ScheduleImportController(ScheduleImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    public ResponseEntity<ScheduleImportDto.ImportPreviewResponse> preview(
            @RequestBody ScheduleImportDto.ImportRequest req) {
        return ResponseEntity.ok(importService.preview(req));
    }

    @PostMapping("/commit")
    public ResponseEntity<ScheduleImportDto.ImportCommitResponse> commit(
            @RequestBody ScheduleImportDto.ImportRequest req) {
        return ResponseEntity.ok(importService.commit(req));
    }
}
