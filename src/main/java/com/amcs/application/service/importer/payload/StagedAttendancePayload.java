package com.amcs.application.service.importer.payload;

import com.amcs.domain.attendance.AttendanceStatus;

import java.util.UUID;

public record StagedAttendancePayload(
    UUID sessionId,
    UUID studentId,
    AttendanceStatus status
) {}
