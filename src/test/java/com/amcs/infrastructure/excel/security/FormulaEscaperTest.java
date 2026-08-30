package com.amcs.infrastructure.excel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FormulaEscaper CWE-1236 Injection Prevention Tests")
class FormulaEscaperTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "=SUM(A1:A10)",
        "=cmd|' /C calc'!A0",
        "-1+1",
        "-50",
        "+cmd|' /C calc'!A0",
        "+100",
        "@HYPERLINK(\"http://malicious.site\", \"Click Me\")",
        "\tmalicious_tab",
        "\rmalicious_cr"
    })
    @DisplayName("Dangerous formula prefixes are safely escaped with a leading single quote")
    void shouldEscapeDangerousPrefixes(String input) {
        String escaped = FormulaEscaper.escape(input);
        assertThat(escaped).startsWith("'");
        assertThat(escaped.substring(1)).isEqualTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "CS2026-001",
        "Alice Student",
        "alice@univ.edu",
        "12345",
        "Normal Text Without Prefix",
        "A regular string with spaces"
    })
    @DisplayName("Normal safe strings remain untouched")
    void shouldNotEscapeSafeStrings(String input) {
        String escaped = FormulaEscaper.escape(input);
        assertThat(escaped).isEqualTo(input);
    }

    @Test
    @DisplayName("Null or empty strings are handled safely without exception")
    void shouldHandleNullAndEmpty() {
        assertThat(FormulaEscaper.escape(null)).isNull();
        assertThat(FormulaEscaper.escape("")).isEmpty();
    }
}
