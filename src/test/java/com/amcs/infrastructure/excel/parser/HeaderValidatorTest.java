package com.amcs.infrastructure.excel.parser;

import com.amcs.application.port.out.excel.HeaderValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HeaderValidator Unit Tests")
class HeaderValidatorTest {

    private final List<String> requiredHeaders = List.of(
        "Registration Number",
        "Full Name",
        "Email",
        "Department Code",
        "Section Name"
    );

    private final List<String> optionalHeaders = List.of(
        "Lab Group Name"
    );

    @Test
    @DisplayName("Exact valid headers pass validation")
    void shouldPassValidHeaders() {
        List<String> headers = List.of(
            "Registration Number", "Full Name", "Email", "Department Code", "Section Name"
        );

        HeaderValidationResult result = HeaderValidator.validate(headers, requiredHeaders, optionalHeaders);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.missingHeaders()).isEmpty();
        assertThat(result.headerToColumnIndex()).containsKeys("registration number", "full name", "email");
    }

    @Test
    @DisplayName("Missing required header is detected and reported")
    void shouldDetectMissingRequiredHeader() {
        List<String> headers = List.of(
            "Registration Number", "Full Name", "Email" // Missing Department Code and Section Name
        );

        HeaderValidationResult result = HeaderValidator.validate(headers, requiredHeaders, optionalHeaders);

        assertThat(result.isValid()).isFalse();
        assertThat(result.missingHeaders()).containsExactlyInAnyOrder("Department Code", "Section Name");
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().getFirst().errorCode()).isEqualTo("MISSING_REQUIRED_HEADER");
    }

    @Test
    @DisplayName("Duplicate headers are detected and rejected")
    void shouldDetectDuplicateHeaders() {
        List<String> headers = List.of(
            "Registration Number", "Full Name", "Email", "Department Code", "Section Name", "Email"
        );

        HeaderValidationResult result = HeaderValidator.validate(headers, requiredHeaders, optionalHeaders);

        assertThat(result.isValid()).isFalse();
        assertThat(result.duplicateHeaders()).contains("Email");
        assertThat(result.errors()).anyMatch(e -> "DUPLICATE_HEADER".equals(e.errorCode()));
    }

    @Test
    @DisplayName("Empty header cell is detected and flagged")
    void shouldDetectEmptyHeader() {
        List<String> headers = List.of(
            "Registration Number", "", "Email", "Department Code", "Section Name"
        );

        HeaderValidationResult result = HeaderValidator.validate(headers, requiredHeaders, optionalHeaders);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "EMPTY_HEADER".equals(e.errorCode()));
    }

    @Test
    @DisplayName("Completely empty header row is rejected")
    void shouldRejectEmptyHeaderRow() {
        HeaderValidationResult result = HeaderValidator.validate(List.of(), requiredHeaders, optionalHeaders);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().getFirst().errorCode()).isEqualTo("EMPTY_HEADER_ROW");
    }
}
