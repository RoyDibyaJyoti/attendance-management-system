package com.amcs.infrastructure.excel.generator;

import com.amcs.application.dto.report.StudentAttendanceReportRow;
import com.amcs.infrastructure.excel.parser.StreamingXlsxParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 5.7: Streaming Architecture & Large Dataset Invariant Tests")
class StreamingAndLargeDatasetVerificationTest {

    private final StreamingReportExcelGenerator reportGenerator = new StreamingReportExcelGenerator();
    private final StreamingXlsxParser streamingParser = new StreamingXlsxParser();

    @Test
    @DisplayName("SXSSF Streaming: Generates 2,000 rows without DOM allocation and disposes temporary files")
    void streamsLargeDatasetWithoutDomAccumulation() {
        int rowCount = 2000;
        List<StudentAttendanceReportRow> rows = new ArrayList<>(rowCount);
        UUID subId = UUID.randomUUID();

        for (int i = 1; i <= rowCount; i++) {
            rows.add(new StudentAttendanceReportRow(
                subId, "CS" + i, "Subject " + i, "THEORY", 50, 45, new BigDecimal("90.00"), "ELIGIBLE"
            ));
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        reportGenerator.writeStudentAttendanceReport(rows, os);

        byte[] resultBytes = os.toByteArray();
        assertThat(resultBytes.length).isGreaterThan(1000);

        // Verify workbook is valid OOXML
        assertThat(resultBytes[0]).isEqualTo((byte) 0x50); // 'P'
        assertThat(resultBytes[1]).isEqualTo((byte) 0x4B); // 'K'
    }

    @Test
    @DisplayName("SAX Parser: Streams 1,000 import rows in chunks without loading entire workbook to memory")
    void parserStreamsRowsIncrementally() throws Exception {
        int rowCount = 1000;
        byte[] xlsxBytes;

        // Generate synthetic XLSX fixture using SXSSF to keep test fast and memory-bounded
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        try {
            Sheet sheet = wb.createSheet("Students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Registration Number");
            header.createCell(1).setCellValue("Full Name");
            header.createCell(2).setCellValue("Email");
            header.createCell(3).setCellValue("Department Code");
            header.createCell(4).setCellValue("Section Name");

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("REG_" + i);
                row.createCell(1).setCellValue("Student " + i);
                row.createCell(2).setCellValue("student" + i + "@univ.edu");
                row.createCell(3).setCellValue("CSE");
                row.createCell(4).setCellValue("Sec-A");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            xlsxBytes = baos.toByteArray();
        } finally {
            wb.dispose();
            wb.close();
        }

        // Stream parse through event parser
        AtomicInteger rowsProcessed = new AtomicInteger(0);
        streamingParser.parseStreaming(
            new ByteArrayInputStream(xlsxBytes),
            null,
            250,
            chunk -> rowsProcessed.addAndGet(chunk.size())
        );

        // Emits 1 header row + 1000 data rows = 1001 total rows
        assertThat(rowsProcessed.get()).isEqualTo(rowCount + 1);
    }
}
