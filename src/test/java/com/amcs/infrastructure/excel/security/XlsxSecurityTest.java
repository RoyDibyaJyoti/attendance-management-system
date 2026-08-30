package com.amcs.infrastructure.excel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("XLSX Security & Anti-Malware Tests")
class XlsxSecurityTest {

    private final XlsxSecurityInspector inspector = new XlsxSecurityInspector();

    @Nested
    @DisplayName("ZIP Archive Integrity & Magic Byte Tests")
    class ArchiveInspectionTests {

        @Test
        @DisplayName("Rejects plain text or non-ZIP magic bytes")
        void shouldRejectNonZipFile() {
            byte[] plainText = "This is definitely not an Excel file".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(plainText)))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("magic bytes");
        }

        @Test
        @DisplayName("Rejects empty input stream")
        void shouldRejectEmptyStream() {
            byte[] empty = new byte[0];

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(empty)))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("too short");
        }

        @Test
        @DisplayName("Rejects ZIP missing essential OOXML structure")
        void shouldRejectNonOoxmlZip() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("random.txt"));
                zos.write("hello".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("missing essential OOXML structure");
        }

        @Test
        @DisplayName("Rejects ZIP containing directory traversal paths")
        void shouldRejectDirectoryTraversal() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("../../etc/passwd"));
                zos.write("root:x:0:0:...".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("Suspicious archive entry path detected");
        }

        @Test
        @DisplayName("Rejects archive exceeding maximum compressed size")
        void shouldRejectOversizedCompressedFile() {
            XlsxSecurityInspector strictInspector = new XlsxSecurityInspector(500, 10000, 10, 10.0);
            byte[] largeDummy = new byte[1000];

            assertThatThrownBy(() -> strictInspector.inspectAndBuffer(new ByteArrayInputStream(largeDummy)))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("exceeds maximum allowed size");
        }
    }

    @Nested
    @DisplayName("XXE & XML Vulnerability Tests")
    class XxeHardeningTests {

        @Test
        @DisplayName("Rejects XML with DOCTYPE declaration (XXE Prevention)")
        void shouldRejectDoctypeDeclaration() throws Exception {
            XMLReader reader = SecureXmlReaderFactory.createSecureXmlReader();

            String xxePayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [
                  <!ELEMENT foo ANY >
                  <!ENTITY xxe SYSTEM "file:///etc/passwd" >]>
                <foo>&xxe;</foo>
                """;

            assertThatThrownBy(() -> reader.parse(new InputSource(new StringReader(xxePayload))))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed");
        }

        @Test
        @DisplayName("Rejects external general entity injection")
        void shouldRejectExternalEntities() throws Exception {
            XMLReader reader = SecureXmlReaderFactory.createSecureXmlReader();

            String payload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                  <!ENTITY ext SYSTEM "http://127.0.0.1:8080/evil.dtd">
                ]>
                <root>&ext;</root>
                """;

            assertThatThrownBy(() -> reader.parse(new InputSource(new StringReader(payload))))
                .isInstanceOf(SAXParseException.class);
        }
    }
}
