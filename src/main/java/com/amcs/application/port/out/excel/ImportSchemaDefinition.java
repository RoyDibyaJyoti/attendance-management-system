package com.amcs.application.port.out.excel;

import com.amcs.domain.importer.ImportType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Standard schema column definitions for bulk Excel ingestion across AMCS entities.
 */
public final class ImportSchemaDefinition {

    // STUDENTS Schema
    public static final List<String> STUDENTS_REQUIRED_HEADERS = List.of(
        "Registration Number",
        "Full Name",
        "Email",
        "Department Code",
        "Section Name"
    );

    // SESSIONS Schema
    public static final List<String> SESSIONS_REQUIRED_HEADERS = List.of(
        "Subject Code",
        "Section Name",
        "Faculty Employee ID",
        "Session Date",
        "Session Type",
        "Planned Units"
    );
    public static final List<String> SESSIONS_OPTIONAL_HEADERS = List.of(
        "Lab Group Name"
    );

    // ATTENDANCE_RECORDS Schema
    public static final List<String> ATTENDANCE_REQUIRED_HEADERS = List.of(
        "Session Identifier",
        "Student Identifier",
        "Attendance Status"
    );

    public static final List<String> VALID_ATTENDANCE_STATUSES = List.of(
        "PRESENT",
        "ABSENT",
        "LATE",
        "EXCUSED",
        "DUTY_LEAVE",
        "MEDICAL_LEAVE"
    );

    public static final List<String> VALID_SESSION_TYPES = List.of(
        "THEORY",
        "LAB"
    );

    private ImportSchemaDefinition() {}

    public static List<String> getRequiredHeaders(ImportType importType) {
        return switch (importType) {
            case STUDENTS -> STUDENTS_REQUIRED_HEADERS;
            case SESSIONS -> SESSIONS_REQUIRED_HEADERS;
            case ATTENDANCE_RECORDS -> ATTENDANCE_REQUIRED_HEADERS;
        };
    }

    public static List<String> getOptionalHeaders(ImportType importType) {
        return switch (importType) {
            case STUDENTS, ATTENDANCE_RECORDS -> List.of();
            case SESSIONS -> SESSIONS_OPTIONAL_HEADERS;
        };
    }
}
