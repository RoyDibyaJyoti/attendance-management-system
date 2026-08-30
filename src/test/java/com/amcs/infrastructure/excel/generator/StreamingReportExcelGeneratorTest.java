package com.amcs.infrastructure.excel.generator;

import com.amcs.application.dto.report.AttendanceRegisterReportData;
import com.amcs.application.dto.report.CondonationRegisterReportRow;
import com.amcs.application.dto.report.DefaulterReportRow;
import com.amcs.application.dto.report.FacultyComplianceReportRow;
import com.amcs.application.dto.report.OverallAttendanceReportRow;
import com.amcs.application.dto.report.PredictionReportRow;
import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.application.dto.report.SubjectAttendanceSummaryReportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreamingReportExcelGenerator Tests")
class StreamingReportExcelGeneratorTest {

    private StreamingReportExcelGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StreamingReportExcelGenerator();
    }

    @Test
    @DisplayName("RPT-001: generates valid XLSX and escapes formula injection")
    void shouldGenerateRpt001WithFormulaEscaping() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<StudentAttendanceReportRow> rows = List.of(
            new StudentAttendanceReportRow(
                UUID.randomUUID(), "=SUM(A1:A10)", "@DANGEROUS", "THEORY", 20, 18, new BigDecimal("90.00"), "ELIGIBLE"
            )
        );

        generator.writeStudentAttendanceReport(rows, os);

        byte[] bytes = os.toByteArray();
        assertThat(bytes.length).isGreaterThan(100);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Student Attendance");
            assertThat(sheet).isNotNull();
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Subject Code");

            Row data = sheet.getRow(1);
            // Verify Formula Injection Sanitization
            assertThat(data.getCell(0).getStringCellValue()).startsWith("'");
            assertThat(data.getCell(1).getStringCellValue()).startsWith("'");
            assertThat(data.getCell(3).getNumericCellValue()).isEqualTo(20.0);
            assertThat(data.getCell(4).getNumericCellValue()).isEqualTo(18.0);
        }
    }

    @Test
    @DisplayName("RPT-002: generates valid XLSX subject summary")
    void shouldGenerateRpt002() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<SubjectAttendanceSummaryReportRow> rows = List.of(
            new SubjectAttendanceSummaryReportRow(
                UUID.randomUUID(), "REG001", "Alice Smith", "Computer Science", 25, 23, new BigDecimal("92.00"), "ELIGIBLE"
            )
        );

        generator.writeSubjectAttendanceSummaryReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Subject Summary");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("REG001");
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("Alice Smith");
        }
    }

    @Test
    @DisplayName("RPT-003: generates valid XLSX defaulter report")
    void shouldGenerateRpt003() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<DefaulterReportRow> rows = List.of(
            new DefaulterReportRow(
                UUID.randomUUID(), "REG002", "Bob Jones", "CS201", "Data Structures", "Sec-A",
                new BigDecimal("60.00"), new BigDecimal("75.00"), 4, 6
            )
        );

        generator.writeDefaulterReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Defaulter Report");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("Bob Jones");
            assertThat(data.getCell(7).getNumericCellValue()).isEqualTo(4.0);
            assertThat(data.getCell(8).getNumericCellValue()).isEqualTo(6.0);
        }
    }

    @Test
    @DisplayName("RPT-004: generates valid XLSX attendance register grid")
    void shouldGenerateRpt004() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        LocalDate d1 = LocalDate.of(2026, 9, 1);
        LocalDate d2 = LocalDate.of(2026, 9, 2);

        AttendanceRegisterReportData.StudentRegisterRow sRow = new AttendanceRegisterReportData.StudentRegisterRow(
            UUID.randomUUID(), "REG001", "Alice",
            Map.of(d1, "PRESENT", d2, "ABSENT"), 2, 1, new BigDecimal("50.00")
        );

        AttendanceRegisterReportData data = new AttendanceRegisterReportData(
            "CS101", "Prog", "A", List.of(d1, d2), List.of(sRow)
        );

        generator.writeAttendanceRegisterReport(data, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Attendance Register");
            assertThat(sheet).isNotNull();
            Row header = sheet.getRow(0);
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("2026-09-01");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("2026-09-02");

            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("REG001");
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("PRESENT");
            assertThat(row1.getCell(3).getStringCellValue()).isEqualTo("ABSENT");
            assertThat(row1.getCell(4).getNumericCellValue()).isEqualTo(2.0);
            assertThat(row1.getCell(5).getNumericCellValue()).isEqualTo(1.0);
        }
    }

    @Test
    @DisplayName("RPT-005: generates valid XLSX overall attendance summary")
    void shouldGenerateRpt005() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<OverallAttendanceReportRow> rows = List.of(
            new OverallAttendanceReportRow(
                UUID.randomUUID(), "REG001", "Alice", "Section-A", 6, 120, 108,
                new BigDecimal("90.00"), new BigDecimal("75.00"), "ELIGIBLE"
            )
        );

        generator.writeOverallAttendanceSummaryReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Overall Attendance Summary");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("REG001");
            assertThat(data.getCell(3).getNumericCellValue()).isEqualTo(6.0);
        }
    }

    @Test
    @DisplayName("RPT-006: generates valid XLSX faculty marking compliance report")
    void shouldGenerateRpt006() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<FacultyComplianceReportRow> rows = List.of(
            new FacultyComplianceReportRow(
                UUID.randomUUID(), "EMP101", "+Dr. Newton", "CSE", "CS301", "Algorithms", "Sec-B",
                40, 38, 2, new BigDecimal("95.00")
            )
        );

        generator.writeFacultyComplianceReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Faculty Compliance");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            // Verify + formula escaping
            assertThat(data.getCell(1).getStringCellValue()).startsWith("'");
            assertThat(data.getCell(7).getNumericCellValue()).isEqualTo(38.0);
            assertThat(data.getCell(8).getNumericCellValue()).isEqualTo(2.0);
        }
    }

    @Test
    @DisplayName("RPT-007: generates valid XLSX condonation register")
    void shouldGenerateRpt007() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<CondonationRegisterReportRow> rows = List.of(
            new CondonationRegisterReportRow(
                UUID.randomUUID(), LocalDate.of(2026, 9, 10), "REG001", "Alice", "CS101", "Prog", "Sec-A",
                "Prof. Smith", "DUTY_LEAVE", Instant.now()
            )
        );

        generator.writeCondonationRegisterReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Condonation Register");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("REG001");
            assertThat(data.getCell(7).getStringCellValue()).isEqualTo("DUTY_LEAVE");
        }
    }

    @Test
    @DisplayName("RPT-008: generates valid XLSX prediction report")
    void shouldGenerateRpt008() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List<PredictionReportRow> rows = List.of(
            new PredictionReportRow(
                UUID.randomUUID(), "-REG003", "Charlie", "CS101", "Prog", 20, 10,
                new BigDecimal("50.00"), new BigDecimal("75.00"), 20, 13, "ACHIEVABLE"
            )
        );

        generator.writePredictionReport(rows, os);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()))) {
            Sheet sheet = wb.getSheet("Prediction Report");
            assertThat(sheet).isNotNull();
            Row data = sheet.getRow(1);
            // Verify - formula escaping
            assertThat(data.getCell(0).getStringCellValue()).startsWith("'");
            assertThat(data.getCell(9).getNumericCellValue()).isEqualTo(13.0);
            assertThat(data.getCell(10).getStringCellValue()).isEqualTo("ACHIEVABLE");
        }
    }
}
