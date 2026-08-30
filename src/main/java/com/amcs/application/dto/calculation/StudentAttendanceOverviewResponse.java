package com.amcs.application.dto.calculation;

import java.util.List;
import java.util.UUID;

public record StudentAttendanceOverviewResponse(
    UUID studentId,
    String studentName,
    String registrationNumber,
    UUID academicPeriodId,
    List<SubjectAttendanceSummaryResponse> subjects
) {}
