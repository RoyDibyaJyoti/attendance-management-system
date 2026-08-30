package com.amcs.infrastructure.excel.parser;

import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.infrastructure.excel.security.InvalidSpreadsheetException;
import com.amcs.infrastructure.excel.security.SecureXmlReaderFactory;
import com.amcs.infrastructure.excel.security.XlsxSecurityInspector;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.Comments;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Memory-bounded, SAX event-driven streaming parser for XLSX workbooks.
 *
 * <p>Never constructs a DOM-based {@code XSSFWorkbook} in memory.
 * Emits parsed rows incrementally in batches (default chunk size 250 rows).
 */
public class StreamingXlsxParser {

    private final XlsxSecurityInspector securityInspector;

    public StreamingXlsxParser() {
        this.securityInspector = new XlsxSecurityInspector();
    }

    public StreamingXlsxParser(XlsxSecurityInspector securityInspector) {
        this.securityInspector = securityInspector;
    }

    /**
     * Streams rows from the first sheet (or specified sheet) of an XLSX spreadsheet.
     *
     * @param inputStream     raw spreadsheet input stream
     * @param sheetNameOrNull name of worksheet to parse, or null for first worksheet
     * @param chunkSize       batch size for emitting rows
     * @param batchConsumer   consumer receiving row batches
     */
    public void parseStreaming(
        InputStream inputStream,
        String sheetNameOrNull,
        int chunkSize,
        Consumer<List<ParsedRow>> batchConsumer
    ) {
        if (chunkSize <= 0) {
            chunkSize = 250;
        }

        byte[] validatedBytes = securityInspector.inspectAndBuffer(inputStream);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(validatedBytes);
             OPCPackage pkg = OPCPackage.open(bais)) {

            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            StylesTable styles = reader.getStylesTable();

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            InputStream targetSheetStream = null;
            Comments sheetComments = null;

            while (sheets.hasNext()) {
                InputStream sheetStream = sheets.next();
                String name = sheets.getSheetName();

                if (sheetNameOrNull == null || sheetNameOrNull.equalsIgnoreCase(name)) {
                    targetSheetStream = sheetStream;
                    sheetComments = sheets.getSheetComments();
                    break;
                }
            }

            if (targetSheetStream == null) {
                throw new InvalidSpreadsheetException(
                    "WORKSHEET_NOT_FOUND",
                    "Target worksheet '" + sheetNameOrNull + "' was not found in workbook"
                );
            }

            try (InputStream is = targetSheetStream) {
                XMLReader xmlReader = SecureXmlReaderFactory.createSecureXmlReader();
                DataFormatter formatter = new DataFormatter();

                SheetStreamingHandler contentsHandler = new SheetStreamingHandler(chunkSize, batchConsumer);
                XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(
                    styles,
                    sheetComments,
                    sst,
                    contentsHandler,
                    formatter,
                    false
                );

                xmlReader.setContentHandler(handler);
                xmlReader.parse(new InputSource(is));

                contentsHandler.flush();
            }

        } catch (InvalidSpreadsheetException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSpreadsheetException("PARSER_ERROR", "Failed to parse XLSX streaming data: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts only Row 1 headers from the first sheet.
     */
    public List<String> extractHeaders(InputStream inputStream) {
        List<String> headers = new ArrayList<>();
        parseStreaming(inputStream, null, 1, batch -> {
            if (headers.isEmpty() && !batch.isEmpty()) {
                // Return raw values from Row 1
                headers.addAll(batch.getFirst().rawValues());
            }
        });
        return headers;
    }

    private static class SheetStreamingHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final int chunkSize;
        private final Consumer<List<ParsedRow>> batchConsumer;
        private final List<ParsedRow> currentBatch = new ArrayList<>();

        private int currentRowIndex = -1;
        private int currentColumnIndex = -1;
        private List<String> currentRowValues = new ArrayList<>();
        private List<String> headerNames = new ArrayList<>();
        private boolean isFirstRow = true;

        public SheetStreamingHandler(int chunkSize, Consumer<List<ParsedRow>> batchConsumer) {
            this.chunkSize = chunkSize;
            this.batchConsumer = batchConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            this.currentRowIndex = rowNum; // 0-based
            this.currentColumnIndex = -1;
            this.currentRowValues = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            int displayRow = rowNum + 1; // 1-based

            if (isFirstRow) {
                this.headerNames = new ArrayList<>(currentRowValues);
                this.isFirstRow = false;

                // Also emit row 1 as a ParsedRow so header inspection can view it
                Map<String, String> headerMap = new HashMap<>();
                for (int i = 0; i < headerNames.size(); i++) {
                    headerMap.put("col_" + i, headerNames.get(i));
                }
                boolean isEmpty = currentRowValues.stream().allMatch(s -> s == null || s.trim().isEmpty());
                emitRow(new ParsedRow(displayRow, headerMap, currentRowValues, isEmpty));
                return;
            }

            Map<String, String> rowMap = new HashMap<>();
            for (int i = 0; i < headerNames.size(); i++) {
                String header = headerNames.get(i);
                if (header != null && !header.trim().isEmpty()) {
                    String norm = header.trim().toLowerCase();
                    String val = (i < currentRowValues.size()) ? currentRowValues.get(i) : "";
                    rowMap.put(norm, val);
                }
            }

            boolean isEmpty = currentRowValues.stream().allMatch(s -> s == null || s.trim().isEmpty());
            emitRow(new ParsedRow(displayRow, rowMap, currentRowValues, isEmpty));
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int colIndex = (cellReference != null) ? new CellReference(cellReference).getCol() : currentColumnIndex + 1;

            // Pad blank cells if any were skipped
            while (currentColumnIndex < colIndex - 1) {
                currentRowValues.add("");
                currentColumnIndex++;
            }

            currentRowValues.add(formattedValue != null ? formattedValue : "");
            currentColumnIndex = colIndex;
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Ignored
        }

        private void emitRow(ParsedRow row) {
            currentBatch.add(row);
            if (currentBatch.size() >= chunkSize) {
                batchConsumer.accept(Collections.unmodifiableList(new ArrayList<>(currentBatch)));
                currentBatch.clear();
            }
        }

        public void flush() {
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(Collections.unmodifiableList(new ArrayList<>(currentBatch)));
                currentBatch.clear();
            }
        }
    }
}
