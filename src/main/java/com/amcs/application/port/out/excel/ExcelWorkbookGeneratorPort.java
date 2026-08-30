package com.amcs.application.port.out.excel;

import com.amcs.domain.importer.ImportType;

/**
 * Outbound port for streaming Excel (XLSX) workbook generation (templates and export reports).
 */
public interface ExcelWorkbookGeneratorPort {

    /**
     * Generates a starter XLSX template for the specified import type, containing standard headers,
     * cell validation dropdowns, date formatting, and illustrative sample data rows.
     *
     * @param importType the target bulk entity type
     * @return raw bytes of the generated .xlsx workbook
     */
    byte[] generateTemplate(ImportType importType);

    /**
     * Generates a streaming XLSX error report summarizing all row validation failures.
     *
     * @param errors list of row validation errors
     * @return raw bytes of the generated .xlsx error report
     */
    byte[] generateErrorReport(java.util.List<com.amcs.domain.importer.RowValidationError> errors);
}
