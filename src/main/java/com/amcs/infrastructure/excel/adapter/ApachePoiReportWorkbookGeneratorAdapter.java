package com.amcs.infrastructure.excel.adapter;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import com.amcs.application.port.out.report.ReportWorkbookGeneratorPort;
import com.amcs.infrastructure.excel.generator.StreamingReportExcelGenerator;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

@Component
public class ApachePoiReportWorkbookGeneratorAdapter implements ReportWorkbookGeneratorPort {

    private final StreamingReportExcelGenerator generator;

    public ApachePoiReportWorkbookGeneratorAdapter() {
        this.generator = new StreamingReportExcelGenerator();
    }

    public ApachePoiReportWorkbookGeneratorAdapter(StreamingReportExcelGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
    }

    @Override
    public void writeStudentAttendanceReport(List<StudentAttendanceReportRow> rows, OutputStream outputStream) {
        generator.writeStudentAttendanceReport(rows, outputStream);
    }

    @Override
    public void writeSubjectAttendanceSummaryReport(List<SubjectAttendanceSummaryReportRow> rows, OutputStream outputStream) {
        generator.writeSubjectAttendanceSummaryReport(rows, outputStream);
    }

    @Override
    public void writeDefaulterReport(List<DefaulterReportRow> rows, OutputStream outputStream) {
        generator.writeDefaulterReport(rows, outputStream);
    }

    @Override
    public void writeAttendanceRegisterReport(AttendanceRegisterReportData data, OutputStream outputStream) {
        generator.writeAttendanceRegisterReport(data, outputStream);
    }

    @Override
    public void writeOverallAttendanceSummaryReport(List<OverallAttendanceReportRow> rows, OutputStream outputStream) {
        generator.writeOverallAttendanceSummaryReport(rows, outputStream);
    }

    @Override
    public void writeFacultyComplianceReport(List<FacultyComplianceReportRow> rows, OutputStream outputStream) {
        generator.writeFacultyComplianceReport(rows, outputStream);
    }

    @Override
    public void writeCondonationRegisterReport(List<CondonationRegisterReportRow> rows, OutputStream outputStream) {
        generator.writeCondonationRegisterReport(rows, outputStream);
    }

    @Override
    public void writePredictionReport(List<PredictionReportRow> rows, OutputStream outputStream) {
        generator.writePredictionReport(rows, outputStream);
    }
}
