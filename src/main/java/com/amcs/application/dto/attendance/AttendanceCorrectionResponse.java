package com.amcs.application.dto.attendance;

import java.time.Instant;
import java.util.UUID;

public record AttendanceCorrectionResponse(
    UUID recordId,
    UUID sessionId,
    UUID studentId,
    String previousStatus,
    String newStatus,
    String reason,
    UUID approverId,
    Instant correctedAt
) {}
