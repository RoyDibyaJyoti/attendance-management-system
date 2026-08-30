package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.session.CancelSessionRequest;
import com.amcs.application.dto.session.CreateSessionRequest;
import com.amcs.application.dto.session.RescheduleSessionRequest;
import com.amcs.application.dto.session.SessionResponse;
import com.amcs.application.service.SessionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Class and lab timetabling, scheduling, cancellation, and rescheduling")
public class SessionController {

    private final SessionApplicationService sessionService;

    public SessionController(SessionApplicationService sessionService) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService");
    }

    @PostMapping
    @Operation(summary = "Schedule a new class or lab session")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session details by ID")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @GetMapping
    @Operation(summary = "List sessions for a section with pagination")
    public ResponseEntity<PagedResponse<SessionResponse>> listSessions(
        @RequestParam UUID sectionId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(sessionService.listSessionsBySection(sectionId, page, boundedSize));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a scheduled session (prohibited if CONDUCTED)")
    public ResponseEntity<SessionResponse> cancelSession(
        @PathVariable UUID id,
        @Valid @RequestBody CancelSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.cancelSession(id, request));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule a session to a new date/slot (prohibited if CONDUCTED)")
    public ResponseEntity<SessionResponse> rescheduleSession(
        @PathVariable UUID id,
        @Valid @RequestBody RescheduleSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.rescheduleSession(id, request));
    }
}
