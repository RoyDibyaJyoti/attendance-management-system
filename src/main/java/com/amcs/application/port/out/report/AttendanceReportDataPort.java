package com.amcs.application.port.out.report;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceReportDataPort {

    List<StudentAttendanceReportRow> getStudentAttendanceReport(UUID studentId, UUID academicPeriodId, UUID sectionId);

    List<SubjectAttendanceSummaryReportRow> getSubjectAttendanceSummary(UUID subjectId, UUID sectionId, UUID academicPeriodId);

    List<DefaulterReportRow> getDefaulterReport(UUID academicPeriodId, UUID sectionId, UUID subjectId, BigDecimal threshold);

    AttendanceRegisterReportData getAttendanceRegisterReport(UUID subjectId, UUID sectionId, UUID academicPeriodId, LocalDate startDate, LocalDate endDate);

    List<OverallAttendanceReportRow> getOverallAttendanceSummary(UUID academicPeriodId, UUID sectionId, UUID departmentId, BigDecimal threshold);

    List<FacultyComplianceReportRow> getFacultyComplianceReport(UUID academicPeriodId, UUID facultyId, UUID departmentId);

    List<CondonationRegisterReportRow> getCondonationRegisterReport(UUID academicPeriodId, UUID sectionId, UUID subjectId, String leaveType, LocalDate startDate, LocalDate endDate);

    List<PredictionReportRow> getPredictionReport(UUID subjectId, UUID sectionId, UUID academicPeriodId, int projectedFutureSessions, BigDecimal targetThreshold);
}
