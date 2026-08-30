package com.amcs.application.dto.attendance;

import java.time.Instant;
import java.util.UUID;

public record AttendanceRecordResponse(
    UUID id,
    UUID sessionId,
    UUID studentId,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
