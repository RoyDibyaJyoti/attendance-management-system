package com.amcs.application.dto.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SessionAttendanceSummaryResponse(
    UUID sessionId,
    UUID subjectId,
    UUID sectionId,
    LocalDate sessionDate,
    String sessionType,
    int conductedUnits,
    String status,
    int totalRecords,
    int presentCount,
    int absentCount,
    int otherCount,
    List<AttendanceRecordResponse> records
) {}
