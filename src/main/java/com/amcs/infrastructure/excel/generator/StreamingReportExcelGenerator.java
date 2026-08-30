package com.amcs.infrastructure.excel.generator;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import com.amcs.infrastructure.excel.security.FormulaEscaper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StreamingReportExcelGenerator {

    private static final int ROW_ACCESS_WINDOW_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void writeStudentAttendanceReport(List<StudentAttendanceReportRow> rows, OutputStream outputStream) {
        generateReport("Student Attendance", new String[]{
            "Subject Code", "Subject Name", "Course Type", "Conducted Units", "Attended Units", "Attendance %", "Status"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (StudentAttendanceReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.subjectCode(), textStyle);
                writeTextCell(row, 1, r.subjectName(), textStyle);
                writeTextCell(row, 2, r.courseType(), textStyle);
                writeNumericCell(row, 3, r.conductedUnits(), numStyle);
                writeNumericCell(row, 4, r.attendedUnits(), numStyle);
                writePercentageCell(row, 5, r.percentage(), pctStyle);
                writeTextCell(row, 6, r.status(), textStyle);
            }
        });
    }

    public void writeSubjectAttendanceSummaryReport(List<SubjectAttendanceSummaryReportRow> rows, OutputStream outputStream) {
        generateReport("Subject Summary", new String[]{
            "Registration No", "Student Name", "Department", "Conducted Units", "Attended Units", "Attendance %", "Status"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (SubjectAttendanceSummaryReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.registrationNumber(), textStyle);
                writeTextCell(row, 1, r.studentName(), textStyle);
                writeTextCell(row, 2, r.departmentName(), textStyle);
                writeNumericCell(row, 3, r.conductedUnits(), numStyle);
                writeNumericCell(row, 4, r.attendedUnits(), numStyle);
                writePercentageCell(row, 5, r.percentage(), pctStyle);
                writeTextCell(row, 6, r.status(), textStyle);
            }
        });
    }

    public void writeDefaulterReport(List<DefaulterReportRow> rows, OutputStream outputStream) {
        generateReport("Defaulter Report", new String[]{
            "Registration No", "Student Name", "Subject Code", "Subject Name", "Section",
            "Current %", "Threshold %", "Units Short", "Classes Required"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (DefaulterReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.registrationNumber(), textStyle);
                writeTextCell(row, 1, r.studentName(), textStyle);
                writeTextCell(row, 2, r.subjectCode(), textStyle);
                writeTextCell(row, 3, r.subjectName(), textStyle);
                writeTextCell(row, 4, r.sectionName(), textStyle);
                writePercentageCell(row, 5, r.currentPercentage(), pctStyle);
                writePercentageCell(row, 6, r.thresholdPercentage(), pctStyle);
                writeNumericCell(row, 7, r.unitsShort(), numStyle);
                writeNumericCell(row, 8, r.classesRequired(), numStyle);
            }
        });
    }

    public void writeAttendanceRegisterReport(AttendanceRegisterReportData data, OutputStream outputStream) {
        List<LocalDate> dates = data.sessionDates();
        String[] headers = new String[2 + dates.size() + 3];
        headers[0] = "Registration No";
        headers[1] = "Student Name";
        for (int i = 0; i < dates.size(); i++) {
            headers[2 + i] = dates.get(i).format(DATE_FORMATTER);
        }
        headers[2 + dates.size()] = "Total Conducted";
        headers[2 + dates.size() + 1] = "Total Attended";
        headers[2 + dates.size() + 2] = "Attendance %";

        generateReport("Attendance Register", headers, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (AttendanceRegisterReportData.StudentRegisterRow r : data.studentRows()) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.registrationNumber(), textStyle);
                writeTextCell(row, 1, r.studentName(), textStyle);

                int col = 2;
                for (LocalDate d : dates) {
                    String code = r.attendanceByDate().getOrDefault(d, "-");
                    writeTextCell(row, col++, code, textStyle);
                }

                writeNumericCell(row, col++, r.totalConducted(), numStyle);
                writeNumericCell(row, col++, r.totalAttended(), numStyle);
                writePercentageCell(row, col, r.percentage(), pctStyle);
            }
        });
    }

    public void writeOverallAttendanceSummaryReport(List<OverallAttendanceReportRow> rows, OutputStream outputStream) {
        generateReport("Overall Attendance Summary", new String[]{
            "Registration No", "Student Name", "Section", "Subjects Count", "Total Conducted Units",
            "Total Attended Units", "Overall %", "Threshold %", "Status"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (OverallAttendanceReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.registrationNumber(), textStyle);
                writeTextCell(row, 1, r.studentName(), textStyle);
                writeTextCell(row, 2, r.sectionName(), textStyle);
                writeNumericCell(row, 3, r.subjectsCount(), numStyle);
                writeNumericCell(row, 4, r.totalConductedUnits(), numStyle);
                writeNumericCell(row, 5, r.totalAttendedUnits(), numStyle);
                writePercentageCell(row, 6, r.overallPercentage(), pctStyle);
                writePercentageCell(row, 7, r.thresholdPercentage(), pctStyle);
                writeTextCell(row, 8, r.overallStatus(), textStyle);
            }
        });
    }

    public void writeFacultyComplianceReport(List<FacultyComplianceReportRow> rows, OutputStream outputStream) {
        generateReport("Faculty Compliance", new String[]{
            "Employee ID", "Faculty Name", "Department", "Subject Code", "Subject Name",
            "Section", "Total Scheduled", "Conducted Sessions", "Unconducted Sessions", "Compliance %"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (FacultyComplianceReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.employeeId(), textStyle);
                writeTextCell(row, 1, r.facultyName(), textStyle);
                writeTextCell(row, 2, r.departmentName(), textStyle);
                writeTextCell(row, 3, r.subjectCode(), textStyle);
                writeTextCell(row, 4, r.subjectName(), textStyle);
                writeTextCell(row, 5, r.sectionName(), textStyle);
                writeNumericCell(row, 6, r.totalScheduledSessions(), numStyle);
                writeNumericCell(row, 7, r.conductedSessions(), numStyle);
                writeNumericCell(row, 8, r.unconductedSessions(), numStyle);
                writePercentageCell(row, 9, r.compliancePercentage(), pctStyle);
            }
        });
    }

    public void writeCondonationRegisterReport(List<CondonationRegisterReportRow> rows, OutputStream outputStream) {
        generateReport("Condonation Register", new String[]{
            "Session Date", "Registration No", "Student Name", "Subject Code", "Subject Name",
            "Section", "Conducted By Faculty", "Leave Type", "Recorded At"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (CondonationRegisterReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.sessionDate() != null ? r.sessionDate().format(DATE_FORMATTER) : "", textStyle);
                writeTextCell(row, 1, r.registrationNumber(), textStyle);
                writeTextCell(row, 2, r.studentName(), textStyle);
                writeTextCell(row, 3, r.subjectCode(), textStyle);
                writeTextCell(row, 4, r.subjectName(), textStyle);
                writeTextCell(row, 5, r.sectionName(), textStyle);
                writeTextCell(row, 6, r.conductedByFacultyName(), textStyle);
                writeTextCell(row, 7, r.leaveType(), textStyle);
                writeTextCell(row, 8, r.recordedAt() != null ? r.recordedAt().toString() : "", textStyle);
            }
        });
    }

    public void writePredictionReport(List<PredictionReportRow> rows, OutputStream outputStream) {
        generateReport("Prediction Report", new String[]{
            "Registration No", "Student Name", "Subject Code", "Subject Name", "Current Conducted",
            "Current Attended", "Current %", "Target Threshold %", "Projected Future Sessions",
            "Required Future Sessions", "Feasibility"
        }, outputStream, (sheet, textStyle, numStyle, pctStyle, dateStyle) -> {
            int rowIdx = 1;
            for (PredictionReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                writeTextCell(row, 0, r.registrationNumber(), textStyle);
                writeTextCell(row, 1, r.studentName(), textStyle);
                writeTextCell(row, 2, r.subjectCode(), textStyle);
                writeTextCell(row, 3, r.subjectName(), textStyle);
                writeNumericCell(row, 4, r.currentConducted(), numStyle);
                writeNumericCell(row, 5, r.currentAttended(), numStyle);
                writePercentageCell(row, 6, r.currentPercentage(), pctStyle);
                writePercentageCell(row, 7, r.targetThresholdPercentage(), pctStyle);
                writeNumericCell(row, 8, r.projectedFutureSessions(), numStyle);
                writeNumericCell(row, 9, r.requiredFutureSessions(), numStyle);
                writeTextCell(row, 10, r.feasibilityStatus(), textStyle);
            }
        });
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(SXSSFSheet sheet, CellStyle textStyle, CellStyle numStyle, CellStyle pctStyle, CellStyle dateStyle);
    }

    private void generateReport(
        String sheetName,
        String[] headers,
        OutputStream outputStream,
        SheetWriter writer
    ) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
        workbook.setCompressTempFiles(true);

        try {
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            sheet.createFreezePane(0, 1);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle numStyle = createNumericStyle(workbook);
            CellStyle pctStyle = createPercentageStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Write Headers
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Write Data
            writer.write(sheet, textStyle, numStyle, pctStyle, dateStyle);

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to stream report workbook: " + e.getMessage(), e);
        } finally {
            try {
                workbook.dispose();
            } catch (Exception ignored) {
            }
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void writeTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(FormulaEscaper.escape(value != null ? value : ""));
        if (style != null) cell.setCellStyle(style);
    }

    private void writeNumericCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private void writePercentageCell(Row row, int col, BigDecimal percentage, CellStyle style) {
        Cell cell = row.createCell(col);
        if (percentage != null) {
            cell.setCellValue(percentage.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        if (style != null) cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createTextStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }

    private CellStyle createNumericStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private CellStyle createPercentageStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00\"%\""));
        return style;
    }

    private CellStyle createDateStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd"));
        return style;
    }
}
