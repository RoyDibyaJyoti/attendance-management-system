package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record DefaulterReportRow(
    UUID studentId,
    String registrationNumber,
    String studentName,
    String subjectCode,
    String subjectName,
    String sectionName,
    BigDecimal currentPercentage,
    BigDecimal thresholdPercentage,
    int unitsShort,
    int classesRequired
) {}
