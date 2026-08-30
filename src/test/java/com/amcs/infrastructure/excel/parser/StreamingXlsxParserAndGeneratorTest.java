package com.amcs.infrastructure.excel.parser;

import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.application.port.out.excel.ImportSchemaDefinition;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.excel.generator.StreamingExcelGenerator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreamingXlsxParser & Generator End-to-End Tests")
class StreamingXlsxParserAndGeneratorTest {

    private StreamingExcelGenerator generator;
    private StreamingXlsxParser parser;

    @BeforeEach
    void setUp() {
        generator = new StreamingExcelGenerator();
        parser = new StreamingXlsxParser();
    }

    @Test
    @DisplayName("Generate STUDENTS template, parse back, and validate headers and sample row")
    void shouldGenerateAndParseStudentsTemplate() {
        byte[] bytes = generator.generateTemplate(ImportType.STUDENTS);
        assertThat(bytes).isNotEmpty();

        List<String> headers = parser.extractHeaders(new ByteArrayInputStream(bytes));
        HeaderValidationResult headerResult = HeaderValidator.validate(
            headers,
            ImportSchemaDefinition.STUDENTS_REQUIRED_HEADERS,
            List.of()
        );
        assertThat(headerResult.isValid()).isTrue();

        List<ParsedRow> allRows = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(bytes), null, 100, allRows::addAll);

        // Row 1 is header, Row 2 is sample row
        assertThat(allRows).hasSize(2);

        ParsedRow sampleRow = allRows.get(1);
        assertThat(sampleRow.rowIndex()).isEqualTo(2);
        assertThat(sampleRow.isEmpty()).isFalse();
        assertThat(sampleRow.get("Registration Number")).isEqualTo("CS2026-001");
        assertThat(sampleRow.get("Full Name")).isEqualTo("Alice Student");
        assertThat(sampleRow.get("Department Code")).isEqualTo("CSE");
    }

    @Test
    @DisplayName("Generate SESSIONS template and verify session fields")
    void shouldGenerateAndParseSessionsTemplate() {
        byte[] bytes = generator.generateTemplate(ImportType.SESSIONS);

        List<String> headers = parser.extractHeaders(new ByteArrayInputStream(bytes));
        HeaderValidationResult headerResult = HeaderValidator.validate(
            headers,
            ImportSchemaDefinition.SESSIONS_REQUIRED_HEADERS,
            ImportSchemaDefinition.SESSIONS_OPTIONAL_HEADERS
        );
        assertThat(headerResult.isValid()).isTrue();

        List<ParsedRow> allRows = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(bytes), null, 100, allRows::addAll);

        assertThat(allRows).hasSize(2);
        ParsedRow sampleRow = allRows.get(1);
        assertThat(sampleRow.get("Subject Code")).isEqualTo("CS101");
        assertThat(sampleRow.get("Session Date")).isEqualTo("2026-09-01");
        assertThat(sampleRow.get("Session Type")).isEqualTo("THEORY");
    }

    @Test
    @DisplayName("Generate ATTENDANCE_RECORDS template and verify attendance status")
    void shouldGenerateAndParseAttendanceTemplate() {
        byte[] bytes = generator.generateTemplate(ImportType.ATTENDANCE_RECORDS);

        List<String> headers = parser.extractHeaders(new ByteArrayInputStream(bytes));
        HeaderValidationResult headerResult = HeaderValidator.validate(
            headers,
            ImportSchemaDefinition.ATTENDANCE_REQUIRED_HEADERS,
            List.of()
        );
        assertThat(headerResult.isValid()).isTrue();

        List<ParsedRow> allRows = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(bytes), null, 100, allRows::addAll);

        assertThat(allRows).hasSize(2);
        ParsedRow sampleRow = allRows.get(1);
        assertThat(sampleRow.get("Attendance Status")).isEqualTo("PRESENT");
    }

    @Test
    @DisplayName("Verify memory-bounded chunking emits batches according to chunkSize")
    void shouldEmitRowsInConfiguredBatches() throws IOException {
        byte[] bytes = createCustomWorkbook(10); // 1 header row + 10 data rows = 11 rows total

        List<List<ParsedRow>> receivedBatches = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(bytes), null, 4, receivedBatches::add);

        // 11 rows with chunkSize 4 -> batches of 4, 4, 3
        assertThat(receivedBatches).hasSize(3);
        assertThat(receivedBatches.get(0)).hasSize(4);
        assertThat(receivedBatches.get(1)).hasSize(4);
        assertThat(receivedBatches.get(2)).hasSize(3);
    }

    @Test
    @DisplayName("Handles unicode names, non-ASCII characters, and emoji")
    void shouldHandleUnicodeAndEmoji() throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(10);
        SXSSFSheet sheet = wb.createSheet("UnicodeSheet");

        Row hRow = sheet.createRow(0);
        hRow.createCell(0).setCellValue("Name");
        hRow.createCell(1).setCellValue("Note");

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Øyvind Sørum");
        r1.createCell(1).setCellValue("Día de exámenes 🎓");

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("李小龙");
        r2.createCell(1).setCellValue("こんにちは 世界 🌟");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.dispose();
        wb.close();

        List<ParsedRow> rows = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(baos.toByteArray()), null, 10, rows::addAll);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(1).get("Name")).isEqualTo("Øyvind Sørum");
        assertThat(rows.get(1).get("Note")).contains("🎓");
        assertThat(rows.get(2).get("Name")).isEqualTo("李小龙");
        assertThat(rows.get(2).get("Note")).contains("🌟");
    }

    @Test
    @DisplayName("Correctly pads blank cells and identifies empty rows")
    void shouldHandleBlankCellsAndRows() throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(10);
        SXSSFSheet sheet = wb.createSheet("BlanksSheet");

        Row hRow = sheet.createRow(0);
        hRow.createCell(0).setCellValue("ColA");
        hRow.createCell(1).setCellValue("ColB");
        hRow.createCell(2).setCellValue("ColC");

        // Row with missing middle cell B
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("ValA");
        // cell 1 skipped
        r1.createCell(2).setCellValue("ValC");

        // Completely blank row
        sheet.createRow(2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.dispose();
        wb.close();

        List<ParsedRow> rows = new ArrayList<>();
        parser.parseStreaming(new ByteArrayInputStream(baos.toByteArray()), null, 10, rows::addAll);

        assertThat(rows).hasSize(3);
        ParsedRow row1 = rows.get(1);
        assertThat(row1.get("ColA")).isEqualTo("ValA");
        assertThat(row1.get("ColB")).isEmpty();
        assertThat(row1.get("ColC")).isEqualTo("ValC");

        ParsedRow emptyRow = rows.get(2);
        assertThat(emptyRow.isEmpty()).isTrue();
    }

    private byte[] createCustomWorkbook(int dataRowCount) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(10);
        SXSSFSheet sheet = wb.createSheet("DataSheet");

        Row hRow = sheet.createRow(0);
        hRow.createCell(0).setCellValue("ID");
        hRow.createCell(1).setCellValue("Value");

        for (int i = 1; i <= dataRowCount; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue("ID_" + i);
            row.createCell(1).setCellValue("Val_" + i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.dispose();
        wb.close();
        return baos.toByteArray();
    }
}
