package com.amcs.infrastructure.excel.adapter;

import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.application.port.out.excel.ImportSchemaDefinition;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.domain.importer.ImportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApachePoiExcel Adapters Integration Tests")
class ApachePoiExcelAdaptersTest {

    private ApachePoiExcelParsingAdapter parsingAdapter;
    private ApachePoiExcelWorkbookGeneratorAdapter generatorAdapter;

    @BeforeEach
    void setUp() {
        parsingAdapter = new ApachePoiExcelParsingAdapter();
        generatorAdapter = new ApachePoiExcelWorkbookGeneratorAdapter();
    }

    @Test
    @DisplayName("Adapters collaborate: generator generates template, parsing adapter validates and streams")
    void shouldCollaborateBetweenGeneratorAndParserAdapters() {
        byte[] bytes = generatorAdapter.generateTemplate(ImportType.STUDENTS);
        assertThat(bytes).isNotEmpty();

        HeaderValidationResult result = parsingAdapter.validateHeaders(
            new ByteArrayInputStream(bytes),
            ImportSchemaDefinition.STUDENTS_REQUIRED_HEADERS,
            List.of()
        );
        assertThat(result.isValid()).isTrue();

        List<ParsedRow> rows = new ArrayList<>();
        parsingAdapter.parseStreaming(new ByteArrayInputStream(bytes), 50, rows::addAll);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).get("Registration Number")).isEqualTo("CS2026-001");
    }
}
