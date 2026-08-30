package com.amcs.application.port.out.report;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;

import java.io.OutputStream;
import java.util.List;

public interface ReportWorkbookGeneratorPort {

    void writeStudentAttendanceReport(List<StudentAttendanceReportRow> rows, OutputStream outputStream);

    void writeSubjectAttendanceSummaryReport(List<SubjectAttendanceSummaryReportRow> rows, OutputStream outputStream);

    void writeDefaulterReport(List<DefaulterReportRow> rows, OutputStream outputStream);

    void writeAttendanceRegisterReport(AttendanceRegisterReportData data, OutputStream outputStream);

    void writeOverallAttendanceSummaryReport(List<OverallAttendanceReportRow> rows, OutputStream outputStream);

    void writeFacultyComplianceReport(List<FacultyComplianceReportRow> rows, OutputStream outputStream);

    void writeCondonationRegisterReport(List<CondonationRegisterReportRow> rows, OutputStream outputStream);

    void writePredictionReport(List<PredictionReportRow> rows, OutputStream outputStream);
}
