package com.amcs.infrastructure.excel.converter;

import com.amcs.application.port.out.excel.ConversionResult;
import com.amcs.domain.attendance.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CellTypeConverter Unit Tests")
class CellTypeConverterTest {

    @Nested
    @DisplayName("String Conversions")
    class StringTests {

        @Test
        @DisplayName("Trims whitespace from string")
        void shouldTrimWhitespace() {
            ConversionResult<String> result = CellTypeConverter.asString("  Alice   ", "name", 2, true);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).contains("Alice");
        }

        @Test
        @DisplayName("Missing required string produces structured error")
        void shouldProduceErrorOnMissingRequiredString() {
            ConversionResult<String> result = CellTypeConverter.asString("   ", "name", 2, true);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().errorCode()).isEqualTo("REQUIRED_FIELD_MISSING");
            assertThat(result.getError().get().rowIndex()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Integer & Decimal Conversions")
    class NumericTests {

        @Test
        @DisplayName("Valid integer and integer with decimal point parsed cleanly")
        void shouldParseInteger() {
            ConversionResult<Integer> r1 = CellTypeConverter.asInteger("42", "units", 2, true);
            assertThat(r1.isSuccess()).isTrue();
            assertThat(r1.getValue()).contains(42);

            ConversionResult<Integer> r2 = CellTypeConverter.asInteger("15.0", "units", 2, true);
            assertThat(r2.isSuccess()).isTrue();
            assertThat(r2.getValue()).contains(15);
        }

        @Test
        @DisplayName("Invalid integer produces structured error")
        void shouldRejectInvalidInteger() {
            ConversionResult<Integer> r = CellTypeConverter.asInteger("not-a-number", "units", 3, true);
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getError().get().errorCode()).isEqualTo("INVALID_INTEGER");
            assertThat(r.getError().get().rejectedValue()).isEqualTo("not-a-number");
        }

        @Test
        @DisplayName("Valid decimal converted to BigDecimal")
        void shouldParseDecimal() {
            ConversionResult<BigDecimal> r = CellTypeConverter.asDecimal("75.50", "percentage", 4, true);
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getValue()).contains(new BigDecimal("75.50"));
        }
    }

    @Nested
    @DisplayName("Boolean Conversions")
    class BooleanTests {

        @Test
        @DisplayName("Various truthy and falsy representations parsed")
        void shouldParseBooleanValues() {
            assertThat(CellTypeConverter.asBoolean("true", "flag", 2, true).getValue()).contains(true);
            assertThat(CellTypeConverter.asBoolean("YES", "flag", 2, true).getValue()).contains(true);
            assertThat(CellTypeConverter.asBoolean("1", "flag", 2, true).getValue()).contains(true);
            assertThat(CellTypeConverter.asBoolean("false", "flag", 2, true).getValue()).contains(false);
            assertThat(CellTypeConverter.asBoolean("NO", "flag", 2, true).getValue()).contains(false);
            assertThat(CellTypeConverter.asBoolean("0", "flag", 2, true).getValue()).contains(false);
        }

        @Test
        @DisplayName("Unrecognized boolean string produces structured error")
        void shouldRejectInvalidBoolean() {
            ConversionResult<Boolean> r = CellTypeConverter.asBoolean("maybe", "flag", 5, true);
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getError().get().errorCode()).isEqualTo("INVALID_BOOLEAN");
        }
    }

    @Nested
    @DisplayName("Date Conversions")
    class DateTests {

        @Test
        @DisplayName("ISO format date parsed")
        void shouldParseIsoDate() {
            ConversionResult<LocalDate> r = CellTypeConverter.asDate("2026-08-30", "date", 2, true);
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getValue()).contains(LocalDate.of(2026, 8, 30));
        }

        @Test
        @DisplayName("Slash format dates parsed")
        void shouldParseSlashDates() {
            ConversionResult<LocalDate> r1 = CellTypeConverter.asDate("30/08/2026", "date", 2, true);
            assertThat(r1.isSuccess()).isTrue();
            assertThat(r1.getValue()).contains(LocalDate.of(2026, 8, 30));
        }

        @Test
        @DisplayName("Numeric Excel date serial parsed correctly")
        void shouldParseExcelDateSerial() {
            // Excel serial 45534 corresponds to August 30, 2024
            ConversionResult<LocalDate> r = CellTypeConverter.asDate("45534", "date", 2, true);
            assertThat(r.isSuccess()).isTrue();
            assertThat(r.getValue().orElseThrow().getYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("Invalid date string produces structured error")
        void shouldRejectInvalidDate() {
            ConversionResult<LocalDate> r = CellTypeConverter.asDate("invalid-date-format", "date", 2, true);
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getError().get().errorCode()).isEqualTo("INVALID_DATE_FORMAT");
        }
    }

    @Nested
    @DisplayName("Enum Conversions")
    class EnumTests {

        @Test
        @DisplayName("Case-insensitive enum parsing succeeds")
        void shouldParseEnumCaseInsensitively() {
            ConversionResult<AttendanceStatus> r1 = CellTypeConverter.asEnum(
                AttendanceStatus.class, "present", "status", 2, true
            );
            assertThat(r1.isSuccess()).isTrue();
            assertThat(r1.getValue()).contains(AttendanceStatus.PRESENT);

            ConversionResult<AttendanceStatus> r2 = CellTypeConverter.asEnum(
                AttendanceStatus.class, "DUTY_LEAVE", "status", 2, true
            );
            assertThat(r2.isSuccess()).isTrue();
            assertThat(r2.getValue()).contains(AttendanceStatus.DUTY_LEAVE);
        }

        @Test
        @DisplayName("Invalid enum value produces structured error")
        void shouldRejectInvalidEnumValue() {
            ConversionResult<AttendanceStatus> r = CellTypeConverter.asEnum(
                AttendanceStatus.class, "UNKNOWN_STATUS", "status", 2, true
            );
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getError().get().errorCode()).isEqualTo("INVALID_ENUM_VALUE");
        }
    }
}
