package com.amcs.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttendanceRegisterReportData(
    String subjectCode,
    String subjectName,
    String sectionName,
    List<LocalDate> sessionDates,
    List<StudentRegisterRow> studentRows
) {
    public record StudentRegisterRow(
        UUID studentId,
        String registrationNumber,
        String studentName,
        Map<LocalDate, String> attendanceByDate,
        int totalConducted,
        int totalAttended,
        BigDecimal percentage
    ) {}
}
