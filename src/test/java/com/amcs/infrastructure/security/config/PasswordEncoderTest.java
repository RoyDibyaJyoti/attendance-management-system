package com.amcs.infrastructure.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        SecurityCryptoConfig config = new SecurityCryptoConfig();
        passwordEncoder = config.passwordEncoder(12);
    }

    @Test
    @DisplayName("Same plaintext password produces different salt hashes when encoded independently")
    void shouldProduceDifferentHashesForSamePassword() {
        String password = "StrongSecretPassword123!";
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).startsWith("{bcrypt}");
        assertThat(hash2).startsWith("{bcrypt}");
    }

    @Test
    @DisplayName("matches() accepts the correct plaintext password")
    void shouldMatchCorrectPassword() {
        String password = "CorrectPassword123!";
        String hash = passwordEncoder.encode(password);

        assertThat(passwordEncoder.matches(password, hash)).isTrue();
    }

    @Test
    @DisplayName("matches() rejects an incorrect plaintext password")
    void shouldRejectIncorrectPassword() {
        String password = "CorrectPassword123!";
        String hash = passwordEncoder.encode(password);

        assertThat(passwordEncoder.matches("WrongPassword123!", hash)).isFalse();
    }

    @Test
    @DisplayName("Encoded hash length is compatible with VARCHAR(100) database column")
    void shouldFitInVarchar100() {
        String password = "AnyArbitraryPasswordLength123!";
        String hash = passwordEncoder.encode(password);

        assertThat(hash.length()).isLessThanOrEqualTo(100);
    }
}
