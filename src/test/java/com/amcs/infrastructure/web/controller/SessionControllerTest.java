package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.common.PagedResponse;
import com.amcs.application.dto.session.CancelSessionRequest;
import com.amcs.application.dto.session.CreateSessionRequest;
import com.amcs.application.dto.session.RescheduleSessionRequest;
import com.amcs.application.dto.session.SessionResponse;
import com.amcs.application.exception.InvalidSessionTransitionException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.service.SessionApplicationService;
import com.amcs.infrastructure.web.error.GlobalRestExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock private SessionApplicationService sessionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final UUID sessionId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 9, 15);

    @BeforeEach
    void setUp() {
        SessionController controller = new SessionController(sessionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalRestExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/sessions creates session with 201 status")
    void shouldCreateSession() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest(
            subjectId, sectionId, facultyId, periodId, today, "THEORY", 1, null);

        SessionResponse response = new SessionResponse(
            sessionId, subjectId, sectionId, facultyId, today, "THEORY", 1, 0, "SCHEDULED", null, null, 0);

        when(sessionService.createSession(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.plannedUnits").value(1))
            .andExpect(jsonPath("$.conductedUnits").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel returns 200 on valid cancel")
    void shouldCancelSession() throws Exception {
        CancelSessionRequest request = new CancelSessionRequest("Faculty illness");
        SessionResponse response = new SessionResponse(
            sessionId, subjectId, sectionId, facultyId, today, "THEORY", 1, 0, "CANCELLED", null, null, 0);

        when(sessionService.cancelSession(eq(sessionId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/sessions/{id}/cancel", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/cancel returns 422 when session is already CONDUCTED")
    void shouldReturn422WhenCancellingConductedSession() throws Exception {
        CancelSessionRequest request = new CancelSessionRequest("Administrative cancellation attempt");

        when(sessionService.cancelSession(eq(sessionId), any()))
            .thenThrow(new InvalidSessionTransitionException("Cannot cancel an already CONDUCTED session."));

        mockMvc.perform(post("/api/v1/sessions/{id}/cancel", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_SESSION_TRANSITION"));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{id}/reschedule returns 200 with new replacement link")
    void shouldRescheduleSession() throws Exception {
        UUID replacementId = UUID.randomUUID();
        RescheduleSessionRequest request = new RescheduleSessionRequest(today.plusDays(2), "Rescheduled slot");
        SessionResponse response = new SessionResponse(
            sessionId, subjectId, sectionId, facultyId, today, "THEORY", 1, 0, "RESCHEDULED", null, replacementId, 0);

        when(sessionService.rescheduleSession(eq(sessionId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/sessions/{id}/reschedule", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESCHEDULED"))
            .andExpect(jsonPath("$.replacedBySessionId").value(replacementId.toString()));
    }
}
