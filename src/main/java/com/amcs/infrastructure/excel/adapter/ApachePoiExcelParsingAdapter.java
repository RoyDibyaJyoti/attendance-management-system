package com.amcs.infrastructure.excel.adapter;

import com.amcs.application.port.out.excel.ExcelParsingPort;
import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.infrastructure.excel.parser.HeaderValidator;
import com.amcs.infrastructure.excel.parser.StreamingXlsxParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
public class ApachePoiExcelParsingAdapter implements ExcelParsingPort {

    private final StreamingXlsxParser parser;

    public ApachePoiExcelParsingAdapter() {
        this.parser = new StreamingXlsxParser();
    }

    public ApachePoiExcelParsingAdapter(StreamingXlsxParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    @Override
    public HeaderValidationResult validateHeaders(
        InputStream inputStream,
        List<String> requiredHeaders,
        List<String> optionalHeaders
    ) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        List<String> extractedHeaders = parser.extractHeaders(inputStream);
        return HeaderValidator.validate(extractedHeaders, requiredHeaders, optionalHeaders);
    }

    @Override
    public void parseStreaming(
        InputStream inputStream,
        int chunkSize,
        Consumer<List<ParsedRow>> batchConsumer
    ) {
        parseStreaming(inputStream, null, chunkSize, batchConsumer);
    }

    @Override
    public void parseStreaming(
        InputStream inputStream,
        String sheetNameOrNull,
        int chunkSize,
        Consumer<List<ParsedRow>> batchConsumer
    ) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(batchConsumer, "batchConsumer must not be null");
        parser.parseStreaming(inputStream, sheetNameOrNull, chunkSize, batchConsumer);
    }
}
