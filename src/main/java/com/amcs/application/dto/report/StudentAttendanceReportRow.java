package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentAttendanceReportRow(
    UUID subjectId,
    String subjectCode,
    String subjectName,
    String courseType,
    int conductedUnits,
    int attendedUnits,
    BigDecimal percentage,
    String status
) {}
