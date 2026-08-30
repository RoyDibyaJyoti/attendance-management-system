package com.amcs.infrastructure.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private final String validCustomSecret = "my-custom-production-secret-key-that-is-at-least-32-bytes-long!";

    @Test
    @DisplayName("In dev or default profile, development fallback secret is accepted")
    void devProfileAllowsFallbackSecret() {
        JwtProperties properties = new JwtProperties(
            JwtProperties.DEV_FALLBACK_SECRET,
            "amcs-auth-service",
            3600,
            "dev"
        );
        assertThat(properties.getSecret()).isEqualTo(JwtProperties.DEV_FALLBACK_SECRET);
    }

    @Test
    @DisplayName("In prod profile, development fallback secret throws IllegalStateException (F-03)")
    void prodProfileRejectsFallbackSecret() {
        assertThatThrownBy(() -> new JwtProperties(
            JwtProperties.DEV_FALLBACK_SECRET,
            "amcs-auth-service",
            3600,
            "prod"
        ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Production environment MUST explicitly configure AMCS_JWT_SECRET");
    }

    @Test
    @DisplayName("In production profile, development fallback secret throws IllegalStateException")
    void productionProfileRejectsFallbackSecret() {
        assertThatThrownBy(() -> new JwtProperties(
            JwtProperties.DEV_FALLBACK_SECRET,
            "amcs-auth-service",
            3600,
            "production"
        ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Production environment MUST explicitly configure AMCS_JWT_SECRET");
    }

    @Test
    @DisplayName("In prod profile with explicit custom secret, configuration succeeds")
    void prodProfileWithCustomSecretSucceeds() {
        JwtProperties properties = new JwtProperties(
            validCustomSecret,
            "amcs-auth-service",
            3600,
            "prod"
        );
        assertThat(properties.getSecret()).isEqualTo(validCustomSecret);
    }

    @Test
    @DisplayName("Secret shorter than 32 bytes throws IllegalArgumentException")
    void shortSecretThrowsException() {
        assertThatThrownBy(() -> new JwtProperties(
            "too-short",
            "amcs-auth-service",
            3600,
            ""
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 256 bits");
    }
}
