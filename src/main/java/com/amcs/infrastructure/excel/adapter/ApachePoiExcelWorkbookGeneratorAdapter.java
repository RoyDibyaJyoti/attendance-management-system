package com.amcs.infrastructure.excel.adapter;

import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.excel.generator.StreamingExcelGenerator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ApachePoiExcelWorkbookGeneratorAdapter implements ExcelWorkbookGeneratorPort {

    private final StreamingExcelGenerator generator;

    public ApachePoiExcelWorkbookGeneratorAdapter() {
        this.generator = new StreamingExcelGenerator();
    }

    public ApachePoiExcelWorkbookGeneratorAdapter(StreamingExcelGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
    }

    @Override
    public byte[] generateTemplate(ImportType importType) {
        Objects.requireNonNull(importType, "importType must not be null");
        return generator.generateTemplate(importType);
    }

    @Override
    public byte[] generateErrorReport(java.util.List<com.amcs.domain.importer.RowValidationError> errors) {
        return generator.generateErrorReport(errors);
    }
}
