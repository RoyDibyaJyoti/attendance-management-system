package com.amcs.infrastructure.excel.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Pre-ingestion security validator for XLSX packages.
 *
 * <p>Validates ZIP magic bytes, rejects zip bombs, enforces decompression ratio limits,
 * rejects directory traversal attempts, and verifies mandatory OOXML package structure
 * before delegating to Apache POI.
 */
public final class XlsxSecurityInspector {

    private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK\x03\x04
    private static final byte[] EMPTY_ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x05, 0x06}; // PK\x05\x06 (Empty zip)

    public static final long DEFAULT_MAX_COMPRESSED_BYTES = 10L * 1024 * 1024;   // 10 MB
    public static final long DEFAULT_MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024; // 50 MB
    public static final int DEFAULT_MAX_ENTRIES = 500;
    public static final double DEFAULT_MAX_COMPRESSION_RATIO = 20.0;

    private final long maxCompressedBytes;
    private final long maxUncompressedBytes;
    private final int maxEntries;
    private final double maxCompressionRatio;

    public XlsxSecurityInspector() {
        this(
            DEFAULT_MAX_COMPRESSED_BYTES,
            DEFAULT_MAX_UNCOMPRESSED_BYTES,
            DEFAULT_MAX_ENTRIES,
            DEFAULT_MAX_COMPRESSION_RATIO
        );
    }

    public XlsxSecurityInspector(
        long maxCompressedBytes,
        long maxUncompressedBytes,
        int maxEntries,
        double maxCompressionRatio
    ) {
        this.maxCompressedBytes = maxCompressedBytes;
        this.maxUncompressedBytes = maxUncompressedBytes;
        this.maxEntries = maxEntries;
        this.maxCompressionRatio = maxCompressionRatio;
    }

    /**
     * Inspects and buffers the incoming stream, enforcing security constraints.
     *
     * @param inputStream untrusted spreadsheet input stream
     * @return validated byte array of the XLSX package
     * @throws InvalidSpreadsheetException if any security or format rule is violated
     */
    public byte[] inspectAndBuffer(InputStream inputStream) {
        if (inputStream == null) {
            throw new InvalidSpreadsheetException("EMPTY_INPUT", "Input stream must not be null");
        }

        byte[] rawBytes = readBounded(inputStream, maxCompressedBytes);
        validateMagicBytes(rawBytes);
        inspectZipStructure(rawBytes);

        return rawBytes;
    }

    /**
     * Inspects the provided raw byte array, enforcing security constraints.
     *
     * @param rawBytes untrusted spreadsheet byte array
     * @throws InvalidSpreadsheetException if any security or format rule is violated
     */
    public void inspect(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length == 0) {
            throw new InvalidSpreadsheetException("EMPTY_INPUT", "Byte array must not be null or empty");
        }
        if (rawBytes.length > maxCompressedBytes) {
            throw new InvalidSpreadsheetException(
                "FILE_TOO_LARGE",
                "Uploaded spreadsheet exceeds maximum allowed size of " + (maxCompressedBytes / (1024 * 1024)) + "MB"
            );
        }
        validateMagicBytes(rawBytes);
        inspectZipStructure(rawBytes);
    }

    private byte[] readBounded(InputStream inputStream, long maxBytes) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        int read;

        try {
            while ((read = inputStream.read(chunk)) != -1) {
                totalRead += read;
                if (totalRead > maxBytes) {
                    throw new InvalidSpreadsheetException(
                        "FILE_TOO_LARGE",
                        "Uploaded spreadsheet exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + "MB"
                    );
                }
                buffer.write(chunk, 0, read);
            }
        } catch (IOException e) {
            throw new InvalidSpreadsheetException("READ_ERROR", "Error reading spreadsheet stream: " + e.getMessage(), e);
        }

        byte[] bytes = buffer.toByteArray();
        if (bytes.length < 4) {
            throw new InvalidSpreadsheetException("FILE_EMPTY", "Uploaded file is too short to be a valid spreadsheet archive");
        }
        return bytes;
    }

    private void validateMagicBytes(byte[] bytes) {
        boolean isZip = bytes[0] == ZIP_MAGIC[0]
            && bytes[1] == ZIP_MAGIC[1]
            && bytes[2] == ZIP_MAGIC[2]
            && bytes[3] == ZIP_MAGIC[3];

        boolean isEmptyZip = bytes[0] == EMPTY_ZIP_MAGIC[0]
            && bytes[1] == EMPTY_ZIP_MAGIC[1]
            && bytes[2] == EMPTY_ZIP_MAGIC[2]
            && bytes[3] == EMPTY_ZIP_MAGIC[3];

        if (!isZip && !isEmptyZip) {
            throw new InvalidSpreadsheetException(
                "INVALID_ARCHIVE_MAGIC",
                "Uploaded file does not have valid ZIP/OOXML magic bytes. Expected PK signature."
            );
        }
    }

    private void inspectZipStructure(byte[] bytes) {
        Set<String> entryNames = new HashSet<>();
        long totalUncompressedBytes = 0;
        int entryCount = 0;

        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("xlsx_sec_", ".tmp");
            java.nio.file.Files.write(tempFile, bytes);

            try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
                java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
                byte[] readBuffer = new byte[8192];

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    entryCount++;
                    if (entryCount > maxEntries) {
                        throw new InvalidSpreadsheetException(
                            "TOO_MANY_ARCHIVE_ENTRIES",
                            "Archive contains too many entries (exceeds limit of " + maxEntries + ")"
                        );
                    }

                    String name = entry.getName();
                    if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                        throw new InvalidSpreadsheetException(
                            "PATH_TRAVERSAL_DETECTED",
                            "Suspicious archive entry path detected: " + name
                        );
                    }

                    String lowerName = name.toLowerCase(java.util.Locale.ROOT);
                    if (lowerName.endsWith(".exe") || lowerName.endsWith(".dll") || lowerName.endsWith(".bat")
                        || lowerName.endsWith(".sh") || lowerName.endsWith(".bin") || lowerName.endsWith(".jar")
                        || lowerName.endsWith(".war")) {
                        throw new InvalidSpreadsheetException(
                            "DANGEROUS_ARCHIVE_ENTRY",
                            "Archive contains prohibited file extension: " + name
                        );
                    }

                    entryNames.add(name);

                    try (InputStream entryStream = zipFile.getInputStream(entry)) {
                        int n;
                        while ((n = entryStream.read(readBuffer)) != -1) {
                            totalUncompressedBytes += n;
                            if (totalUncompressedBytes > maxUncompressedBytes) {
                                throw new InvalidSpreadsheetException(
                                    "UNCOMPRESSED_LIMIT_EXCEEDED",
                                    "Uncompressed spreadsheet data exceeds maximum allowed limit of "
                                        + (maxUncompressedBytes / (1024 * 1024)) + "MB"
                                );
                            }
                        }
                    }
                }
            }

            // Check overall compression ratio for zip bomb protection
            if (bytes.length > 0) {
                double ratio = (double) totalUncompressedBytes / (double) bytes.length;
                if (ratio > maxCompressionRatio && totalUncompressedBytes > 1024 * 1024) {
                    throw new InvalidSpreadsheetException(
                        "SUSPICIOUS_COMPRESSION_RATIO",
                        String.format("Suspicious compression ratio %.2f:1 detected (limit %.2f:1)", ratio, maxCompressionRatio)
                    );
                }
            }

            // Verify minimum OOXML requirements
            boolean hasContentTypes = entryNames.contains("[Content_Types].xml");
            boolean hasWorkbook = entryNames.contains("xl/workbook.xml");
            if (!hasContentTypes || !hasWorkbook) {
                throw new InvalidSpreadsheetException(
                    "NOT_AN_OOXML_WORKBOOK",
                    "Archive is a valid ZIP but missing essential OOXML structure ([Content_Types].xml or xl/workbook.xml)"
                );
            }

        } catch (InvalidSpreadsheetException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidSpreadsheetException("MALFORMED_ZIP", "Failed to inspect ZIP structure: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
