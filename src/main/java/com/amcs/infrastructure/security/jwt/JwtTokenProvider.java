package com.amcs.infrastructure.security.jwt;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.jwt.InvalidJwtException.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * High-performance, secure JWT provider implementing HS256 generation and parsing.
 * Supports configurable expiration, issuer validation, and deterministic Clock injection for tests.
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey secretKey;

    @Autowired
    public JwtTokenProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Generates a signed HS256 JWT access token from an AuthenticationResult.
     */
    public String generateToken(AuthenticationResult auth) {
        Objects.requireNonNull(auth, "AuthenticationResult must not be null");

        Instant now = clock.instant();
        Instant exp = now.plusSeconds(properties.getExpirationSeconds());

        JwtBuilder builder = Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(auth.userId().toString())
            .claim("username", auth.username())
            .claim("email", auth.email())
            .claim("role", auth.role().name())
            .claim("tokenVersion", auth.tokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(secretKey, Jwts.SIG.HS256);

        auth.studentId().ifPresent(id -> builder.claim("studentId", id.toString()));
        auth.facultyId().ifPresent(id -> builder.claim("facultyId", id.toString()));

        return builder.compact();
    }

    /**
     * Cryptographically validates and parses an access token, returning validated claims.
     */
    public ValidatedTokenClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException(ErrorCode.INVALID_FORMAT, "JWT token must not be null, empty, or blank");
        }

        String rawToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        if (rawToken.isEmpty()) {
            throw new InvalidJwtException(ErrorCode.INVALID_FORMAT, "JWT token must not be empty");
        }

        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(rawToken);
        } catch (ExpiredJwtException ex) {
            throw new InvalidJwtException(ErrorCode.EXPIRED, "JWT token has expired", ex);
        } catch (SecurityException ex) {
            throw new InvalidJwtException(ErrorCode.INVALID_SIGNATURE, "JWT token signature is invalid or tampered", ex);
        } catch (MalformedJwtException ex) {
            throw new InvalidJwtException(ErrorCode.MALFORMED, "JWT token structure is malformed", ex);
        } catch (UnsupportedJwtException ex) {
            throw new InvalidJwtException(ErrorCode.UNSUPPORTED_ALGORITHM, "JWT token algorithm is unsupported", ex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidJwtException(ErrorCode.INVALID_FORMAT, "JWT token argument is invalid: " + ex.getMessage(), ex);
        }

        // Validate algorithm header
        String alg = jws.getHeader().getAlgorithm();
        if (!"HS256".equalsIgnoreCase(alg)) {
            throw new InvalidJwtException(
                ErrorCode.UNSUPPORTED_ALGORITHM,
                "Unsupported JWT algorithm: " + alg + ", expected HS256"
            );
        }

        Claims payload = jws.getPayload();

        // Validate issuer
        String issuer = payload.getIssuer();
        if (!properties.getIssuer().equals(issuer)) {
            throw new InvalidJwtException(ErrorCode.INVALID_ISSUER, "Invalid token issuer: " + issuer);
        }

        // Validate subject (UUID)
        String sub = payload.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing subject claim in token");
        }
        UUID userId;
        try {
            userId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new InvalidJwtException(ErrorCode.MALFORMED, "Subject claim is not a valid UUID: " + sub);
        }

        // Validate username
        String username = payload.get("username", String.class);
        if (username == null || username.isBlank()) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing username claim in token");
        }

        // Validate email
        String email = payload.get("email", String.class);
        if (email == null || email.isBlank()) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing email claim in token");
        }

        // Validate role
        String roleStr = payload.get("role", String.class);
        if (roleStr == null || roleStr.isBlank()) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing role claim in token");
        }
        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidJwtException(ErrorCode.MALFORMED, "Invalid user role claim: " + roleStr);
        }

        // Validate optional studentId
        String studentIdStr = payload.get("studentId", String.class);
        UUID studentId = null;
        if (studentIdStr != null && !studentIdStr.isBlank()) {
            try {
                studentId = UUID.fromString(studentIdStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidJwtException(ErrorCode.MALFORMED, "Malformed studentId UUID: " + studentIdStr);
            }
        }

        // Validate optional facultyId
        String facultyIdStr = payload.get("facultyId", String.class);
        UUID facultyId = null;
        if (facultyIdStr != null && !facultyIdStr.isBlank()) {
            try {
                facultyId = UUID.fromString(facultyIdStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidJwtException(ErrorCode.MALFORMED, "Malformed facultyId UUID: " + facultyIdStr);
            }
        }

        // Validate tokenVersion
        Integer tokenVersion = payload.get("tokenVersion", Integer.class);
        if (tokenVersion == null) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing tokenVersion claim in token");
        }

        Date iat = payload.getIssuedAt();
        Date exp = payload.getExpiration();
        if (iat == null || exp == null) {
            throw new InvalidJwtException(ErrorCode.MISSING_CLAIMS, "Missing iat or exp timestamp claims");
        }

        return new ValidatedTokenClaims(
            userId,
            username,
            email,
            role,
            Optional.ofNullable(studentId),
            Optional.ofNullable(facultyId),
            tokenVersion,
            iat.toInstant(),
            exp.toInstant()
        );
    }

    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (InvalidJwtException e) {
            return false;
        }
    }

    public int extractTokenVersion(String token) {
        return parseAndValidate(token).tokenVersion();
    }
}
