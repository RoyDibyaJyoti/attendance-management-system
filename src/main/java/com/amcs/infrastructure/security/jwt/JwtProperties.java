package com.amcs.infrastructure.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Configuration properties for JWT generation and validation.
 * Enforces minimum 256 bits (32 bytes) of secret entropy.
 */
@Component
public class JwtProperties {

    public static final String DEV_FALLBACK_SECRET =
        "dev-secret-key-for-amcs-local-testing-must-be-at-least-256-bits-long-32-bytes!";

    private final String secret;
    private final String issuer;
    private final long expirationSeconds;

    public JwtProperties(
        String secret,
        String issuer,
        long expirationSeconds
    ) {
        this(secret, issuer, expirationSeconds, "");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtProperties(
        @Value("${amcs.security.jwt.secret}") String secret,
        @Value("${amcs.security.jwt.issuer:amcs-auth-service}") String issuer,
        @Value("${amcs.security.jwt.expiration-seconds:3600}") long expirationSeconds,
        @Value("${spring.profiles.active:}") String activeProfiles
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("AMCS JWT secret must not be blank");
        }
        if (isProductionProfile(activeProfiles) && DEV_FALLBACK_SECRET.equals(secret.trim())) {
            throw new IllegalStateException(
                "Production environment MUST explicitly configure AMCS_JWT_SECRET. " +
                "The development fallback secret is strictly prohibited in production."
            );
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                "AMCS JWT secret must contain at least 256 bits (32 bytes) of entropy. Provided: " + secretBytes.length + " bytes"
            );
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("AMCS JWT issuer must not be blank");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("AMCS JWT expiration must be greater than zero");
        }

        this.secret = secret;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    private static boolean isProductionProfile(String activeProfiles) {
        if (activeProfiles == null || activeProfiles.isBlank()) {
            return false;
        }
        for (String profile : activeProfiles.split(",")) {
            String p = profile.trim().toLowerCase();
            if ("prod".equals(p) || "production".equals(p)) {
                return true;
            }
        }
        return false;
    }

    public String getSecret() {
        return secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
