package com.amcs.infrastructure.excel.converter;

import com.amcs.application.port.out.excel.ConversionResult;
import com.amcs.domain.importer.RowValidationError;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * Reusable converter transforming raw spreadsheet cell strings into strongly typed domain types.
 *
 * <p>Produces structured {@link RowValidationError} records upon conversion failures without
 * throwing generic runtime exceptions.
 */
public final class CellTypeConverter {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,                      // yyyy-MM-dd
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),             // dd/MM/yyyy
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),             // MM/dd/yyyy
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),             // yyyy/MM/dd
        DateTimeFormatter.ofPattern("d-M-yyyy"),               // d-M-yyyy
        DateTimeFormatter.ofPattern("d/M/yyyy")                // d/M/yyyy
    );

    private CellTypeConverter() {}

    /**
     * Converts a raw cell string to a clean trimmed String.
     */
    public static ConversionResult<String> asString(String raw, String columnName, int rowIndex, boolean required) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success("");
        }
        return ConversionResult.success(raw.trim());
    }

    /**
     * Converts a raw cell string to an Integer.
     */
    public static ConversionResult<Integer> asInteger(String raw, String columnName, int rowIndex, boolean required) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success(null);
        }

        String cleaned = raw.trim();
        try {
            // Support numbers stored as floats/scientific in Excel (e.g. "12.0")
            if (cleaned.contains(".")) {
                double d = Double.parseDouble(cleaned);
                if (d == (int) d) {
                    return ConversionResult.success((int) d);
                }
            }
            int val = Integer.parseInt(cleaned);
            return ConversionResult.success(val);
        } catch (NumberFormatException e) {
            return ConversionResult.failure(new RowValidationError(
                rowIndex, columnName, cleaned, "INVALID_INTEGER", "Value '" + cleaned + "' is not a valid integer"
            ));
        }
    }

    /**
     * Converts a raw cell string to a BigDecimal.
     */
    public static ConversionResult<BigDecimal> asDecimal(String raw, String columnName, int rowIndex, boolean required) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success(null);
        }

        String cleaned = raw.trim();
        try {
            BigDecimal val = new BigDecimal(cleaned);
            return ConversionResult.success(val);
        } catch (NumberFormatException e) {
            return ConversionResult.failure(new RowValidationError(
                rowIndex, columnName, cleaned, "INVALID_DECIMAL", "Value '" + cleaned + "' is not a valid decimal number"
            ));
        }
    }

    /**
     * Converts a raw cell string to a Boolean.
     */
    public static ConversionResult<Boolean> asBoolean(String raw, String columnName, int rowIndex, boolean required) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success(null);
        }

        String cleaned = raw.trim().toLowerCase();
        if ("true".equals(cleaned) || "1".equals(cleaned) || "yes".equals(cleaned) || "y".equals(cleaned)) {
            return ConversionResult.success(Boolean.TRUE);
        }
        if ("false".equals(cleaned) || "0".equals(cleaned) || "no".equals(cleaned) || "n".equals(cleaned)) {
            return ConversionResult.success(Boolean.FALSE);
        }

        return ConversionResult.failure(new RowValidationError(
            rowIndex, columnName, raw.trim(), "INVALID_BOOLEAN", "Value '" + raw.trim() + "' is not a valid boolean (expected true/false/yes/no)"
        ));
    }

    /**
     * Converts a raw cell string to a LocalDate, handling ISO formats and Excel date serials.
     */
    public static ConversionResult<LocalDate> asDate(String raw, String columnName, int rowIndex, boolean required) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success(null);
        }

        String cleaned = raw.trim();

        // 1. Try numeric Excel date serial (e.g. 45534)
        try {
            double serial = Double.parseDouble(cleaned);
            if (DateUtil.isValidExcelDate(serial) && serial > 0) {
                Date javaDate = DateUtil.getJavaDate(serial);
                LocalDate localDate = javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return ConversionResult.success(localDate);
            }
        } catch (NumberFormatException ignored) {
            // Not a numeric serial, proceed to text parsing
        }

        // 2. Try standard string date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(cleaned, formatter);
                return ConversionResult.success(parsed);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        return ConversionResult.failure(new RowValidationError(
            rowIndex, columnName, cleaned, "INVALID_DATE_FORMAT",
            "Value '" + cleaned + "' is not a recognized date format (expected YYYY-MM-DD or DD/MM/YYYY)"
        ));
    }

    /**
     * Converts a raw cell string to a specified Enum constant case-insensitively.
     */
    public static <E extends Enum<E>> ConversionResult<E> asEnum(
        Class<E> enumClass,
        String raw,
        String columnName,
        int rowIndex,
        boolean required
    ) {
        if (raw == null || raw.trim().isEmpty()) {
            if (required) {
                return ConversionResult.failure(new RowValidationError(
                    rowIndex, columnName, "", "REQUIRED_FIELD_MISSING", "Field '" + columnName + "' is required"
                ));
            }
            return ConversionResult.success(null);
        }

        String cleaned = raw.trim();
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(cleaned)) {
                return ConversionResult.success(constant);
            }
        }

        return ConversionResult.failure(new RowValidationError(
            rowIndex, columnName, cleaned, "INVALID_ENUM_VALUE",
            "Value '" + cleaned + "' is not valid for field '" + columnName + "'"
        ));
    }
}
