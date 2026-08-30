package com.amcs.infrastructure.excel.generator;

import com.amcs.application.port.out.excel.ImportSchemaDefinition;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.excel.security.FormulaEscaper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Memory-bounded streaming Excel generator producing starter XLSX templates.
 *
 * <p>Uses {@link SXSSFWorkbook} with a sliding window of 100 rows to bound memory usage,
 * and guarantees disposal of temporary backing files in a finally block.
 */
public class StreamingExcelGenerator {

    private static final int ROW_ACCESS_WINDOW_SIZE = 100;

    /**
     * Generates an XLSX template for the given import type.
     *
     * @param importType target import category
     * @return raw bytes of the generated spreadsheet
     */
    public byte[] generateTemplate(ImportType importType) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
        workbook.setCompressTempFiles(true);

        try {
            SXSSFSheet sheet = workbook.createSheet(importType.name());

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);

            List<String> requiredHeaders = ImportSchemaDefinition.getRequiredHeaders(importType);
            List<String> optionalHeaders = ImportSchemaDefinition.getOptionalHeaders(importType);

            // Write Row 0 (Headers)
            Row headerRow = sheet.createRow(0);
            int colIdx = 0;
            for (String req : requiredHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(FormulaEscaper.escape(req));
                cell.setCellStyle(headerStyle);
            }
            for (String opt : optionalHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(FormulaEscaper.escape(opt + " (Optional)"));
                cell.setCellStyle(headerStyle);
            }

            // Write Sample Row (Row 1)
            Row sampleRow = sheet.createRow(1);
            writeSampleRow(importType, sampleRow, dateStyle, textStyle);

            // Setup Data Validation Dropdowns
            setupDataValidations(importType, sheet);

            // Set Reasonable Column Widths (256 units = 1 character)
            for (int i = 0; i < colIdx; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel template: " + e.getMessage(), e);
        } finally {
            try {
                workbook.dispose(); // Delete temporary disk flush files
                workbook.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void writeSampleRow(ImportType importType, Row row, CellStyle dateStyle, CellStyle textStyle) {
        switch (importType) {
            case STUDENTS -> {
                createCell(row, 0, "CS2026-001", textStyle);
                createCell(row, 1, "Alice Student", textStyle);
                createCell(row, 2, "alice.student@university.edu", textStyle);
                createCell(row, 3, "CSE", textStyle);
                createCell(row, 4, "A", textStyle);
            }
            case SESSIONS -> {
                createCell(row, 0, "CS101", textStyle);
                createCell(row, 1, "A", textStyle);
                createCell(row, 2, "FAC001", textStyle);
                createCell(row, 3, "2026-09-01", dateStyle);
                createCell(row, 4, "THEORY", textStyle);
                createCell(row, 5, "1", textStyle);
                createCell(row, 6, "G1", textStyle); // Optional lab group
            }
            case ATTENDANCE_RECORDS -> {
                createCell(row, 0, "SESS-2026-001", textStyle);
                createCell(row, 1, "CS2026-001", textStyle);
                createCell(row, 2, "PRESENT", textStyle);
            }
        }
    }

    private void setupDataValidations(ImportType importType, Sheet sheet) {
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();

        if (importType == ImportType.ATTENDANCE_RECORDS) {
            // Dropdown on Column C (index 2) for Attendance Status
            String[] statuses = ImportSchemaDefinition.VALID_ATTENDANCE_STATUSES.toArray(new String[0]);
            DataValidationConstraint constraint = dvHelper.createExplicitListConstraint(statuses);
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 2, 2);
            DataValidation validation = dvHelper.createValidation(constraint, addressList);
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        } else if (importType == ImportType.SESSIONS) {
            // Dropdown on Column E (index 4) for Session Type
            String[] types = ImportSchemaDefinition.VALID_SESSION_TYPES.toArray(new String[0]);
            DataValidationConstraint constraint = dvHelper.createExplicitListConstraint(types);
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 4, 4);
            DataValidation validation = dvHelper.createValidation(constraint, addressList);
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }
    }

    private void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(FormulaEscaper.escape(value));
        if (style != null) {
            cell.setCellStyle(style);
        }
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

    private CellStyle createDateStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createTextStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }

    /**
     * Generates a streaming XLSX error report summarizing all row validation failures.
     */
    public byte[] generateErrorReport(List<com.amcs.domain.importer.RowValidationError> errors) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
        workbook.setCompressTempFiles(true);

        try {
            SXSSFSheet sheet = workbook.createSheet("Validation Errors");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);

            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = new String[]{"Row Index", "Column Name", "Rejected Value", "Error Code", "Error Message"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (errors != null) {
                int rowNum = 1;
                for (var err : errors) {
                    Row row = sheet.createRow(rowNum++);

                    Cell c0 = row.createCell(0);
                    c0.setCellValue(err.rowIndex());

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(FormulaEscaper.escape(err.columnName() != null ? err.columnName() : ""));
                    c1.setCellStyle(textStyle);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(FormulaEscaper.escape(err.rejectedValue() != null ? err.rejectedValue() : ""));
                    c2.setCellStyle(textStyle);

                    Cell c3 = row.createCell(3);
                    c3.setCellValue(FormulaEscaper.escape(err.errorCode() != null ? err.errorCode() : ""));
                    c3.setCellStyle(textStyle);

                    Cell c4 = row.createCell(4);
                    c4.setCellValue(FormulaEscaper.escape(err.errorMessage() != null ? err.errorMessage() : ""));
                    c4.setCellStyle(textStyle);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate error report workbook: " + e.getMessage(), e);
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
}
