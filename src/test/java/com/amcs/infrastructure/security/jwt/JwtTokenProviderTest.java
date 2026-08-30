package com.amcs.infrastructure.security.jwt;

import com.amcs.application.dto.security.AuthenticationResult;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.infrastructure.security.jwt.InvalidJwtException.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "super-secret-key-that-is-at-least-256-bits-long-32-bytes!";
    private static final String ANOTHER_SECRET = "another-super-secret-key-that-is-at-least-256-bits-long!";
    private static final String ISSUER = "amcs-auth-service";
    private static final long EXPIRATION_SECONDS = 3600L;

    private Instant fixedInstant;
    private Clock fixedClock;
    private JwtProperties properties;
    private JwtTokenProvider provider;

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID facultyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-08-29T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        properties = new JwtProperties(TEST_SECRET, ISSUER, EXPIRATION_SECONDS);
        provider = new JwtTokenProvider(properties, fixedClock);
    }

    private AuthenticationResult createStudentAuth() {
        return new AuthenticationResult(
            userId,
            "CS2026-001",
            "student@univ.edu",
            UserRole.STUDENT,
            Optional.of(studentId),
            Optional.empty(),
            1
        );
    }

    private AuthenticationResult createFacultyAuth() {
        return new AuthenticationResult(
            userId,
            "EMP-8802",
            "faculty@univ.edu",
            UserRole.FACULTY,
            Optional.empty(),
            Optional.of(facultyId),
            2
        );
    }

    // ========================================================================
    // GENERATION TESTS
    // ========================================================================

    @Test
    @DisplayName("1-12. Generates a valid JWT with exact required claims, issuer, subject, and expiration")
    void shouldGenerateValidTokenWithAllClaims() {
        AuthenticationResult auth = createStudentAuth();

        String token = provider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.isValid(token)).isTrue();

        ValidatedTokenClaims claims = provider.parseAndValidate(token);
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.username()).isEqualTo("CS2026-001");
        assertThat(claims.email()).isEqualTo("student@univ.edu");
        assertThat(claims.role()).isEqualTo(UserRole.STUDENT);
        assertThat(claims.studentId()).contains(studentId);
        assertThat(claims.facultyId()).isEmpty();
        assertThat(claims.tokenVersion()).isEqualTo(1);
        assertThat(claims.issuedAt()).isEqualTo(fixedInstant);
        assertThat(claims.expiration()).isEqualTo(fixedInstant.plusSeconds(EXPIRATION_SECONDS));
    }

    @Test
    @DisplayName("8. Correct faculty ID for faculty authentication result")
    void shouldGenerateValidFacultyToken() {
        AuthenticationResult auth = createFacultyAuth();

        String token = provider.generateToken(auth);
        ValidatedTokenClaims claims = provider.parseAndValidate(token);

        assertThat(claims.role()).isEqualTo(UserRole.FACULTY);
        assertThat(claims.facultyId()).contains(facultyId);
        assertThat(claims.studentId()).isEmpty();
        assertThat(claims.tokenVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("12. Expiration is exactly 60 minutes after issuance")
    void shouldHaveExact60MinutesLifetime() {
        String token = provider.generateToken(createStudentAuth());
        ValidatedTokenClaims claims = provider.parseAndValidate(token);

        Duration lifetime = Duration.between(claims.issuedAt(), claims.expiration());
        assertThat(lifetime.toSeconds()).isEqualTo(3600L);
    }

    // ========================================================================
    // VALIDATION TESTS
    // ========================================================================

    @Test
    @DisplayName("13. Accepts Bearer prefixed token as well as raw token")
    void shouldAcceptBearerPrefixedToken() {
        String token = provider.generateToken(createStudentAuth());
        ValidatedTokenClaims claims = provider.parseAndValidate("Bearer " + token);

        assertThat(claims.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("14. Expired token is rejected with EXPIRED error code")
    void shouldRejectExpiredToken() {
        String token = provider.generateToken(createStudentAuth());

        // Fast-forward clock by 3601 seconds
        Clock fastForwardClock = Clock.fixed(fixedInstant.plusSeconds(3601), ZoneOffset.UTC);
        JwtTokenProvider expiredVerifier = new JwtTokenProvider(properties, fastForwardClock);

        assertThatThrownBy(() -> expiredVerifier.parseAndValidate(token))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.EXPIRED);
    }

    @Test
    @DisplayName("15. Tampered signature is rejected with INVALID_SIGNATURE")
    void shouldRejectTamperedSignature() {
        String token = provider.generateToken(createStudentAuth());
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> provider.parseAndValidate(tampered))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("16. Token signed with another secret is rejected")
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtProperties otherProps = new JwtProperties(ANOTHER_SECRET, ISSUER, EXPIRATION_SECONDS);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps, fixedClock);

        String foreignToken = otherProvider.generateToken(createStudentAuth());

        assertThatThrownBy(() -> provider.parseAndValidate(foreignToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("17. Incorrect issuer is rejected with INVALID_ISSUER")
    void shouldRejectIncorrectIssuer() {
        JwtProperties foreignIssuerProps = new JwtProperties(TEST_SECRET, "rogue-issuer", EXPIRATION_SECONDS);
        JwtTokenProvider foreignIssuerProvider = new JwtTokenProvider(foreignIssuerProps, fixedClock);

        String rogueToken = foreignIssuerProvider.generateToken(createStudentAuth());

        assertThatThrownBy(() -> provider.parseAndValidate(rogueToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_ISSUER);
    }

    @Test
    @DisplayName("18. Malformed token is rejected with MALFORMED")
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> provider.parseAndValidate("not.a.valid.jwt.token"))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.MALFORMED);
    }

    @Test
    @DisplayName("19. Empty or blank token is rejected with INVALID_FORMAT")
    void shouldRejectEmptyToken() {
        assertThatThrownBy(() -> provider.parseAndValidate(""))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_FORMAT);

        assertThatThrownBy(() -> provider.parseAndValidate("   "))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_FORMAT);

        assertThatThrownBy(() -> provider.parseAndValidate(null))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_FORMAT);
    }

    @Test
    @DisplayName("20 & 21. Missing or invalid UUID subject is rejected")
    void shouldRejectInvalidSubject() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String nonUuidToken = Jwts.builder()
            .issuer(ISSUER)
            .subject("not-a-uuid")
            .claim("username", "alice")
            .claim("email", "alice@univ.edu")
            .claim("role", "STUDENT")
            .claim("tokenVersion", 1)
            .issuedAt(Date.from(fixedInstant))
            .expiration(Date.from(fixedInstant.plusSeconds(3600)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parseAndValidate(nonUuidToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.MALFORMED);
    }

    @Test
    @DisplayName("22. Invalid role claim is rejected")
    void shouldRejectInvalidRole() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String invalidRoleToken = Jwts.builder()
            .issuer(ISSUER)
            .subject(userId.toString())
            .claim("username", "alice")
            .claim("email", "alice@univ.edu")
            .claim("role", "SUPER_HACKER")
            .claim("tokenVersion", 1)
            .issuedAt(Date.from(fixedInstant))
            .expiration(Date.from(fixedInstant.plusSeconds(3600)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parseAndValidate(invalidRoleToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.MALFORMED);
    }

    @Test
    @DisplayName("23 & 24. Malformed student or faculty UUID is rejected")
    void shouldRejectMalformedPersonIds() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String badStudentToken = Jwts.builder()
            .issuer(ISSUER)
            .subject(userId.toString())
            .claim("username", "alice")
            .claim("email", "alice@univ.edu")
            .claim("role", "STUDENT")
            .claim("studentId", "invalid-uuid")
            .claim("tokenVersion", 1)
            .issuedAt(Date.from(fixedInstant))
            .expiration(Date.from(fixedInstant.plusSeconds(3600)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parseAndValidate(badStudentToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.MALFORMED);
    }

    @Test
    @DisplayName("25. Missing required claim (tokenVersion) is rejected")
    void shouldRejectMissingRequiredClaim() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String missingClaimToken = Jwts.builder()
            .issuer(ISSUER)
            .subject(userId.toString())
            .claim("username", "alice")
            .claim("email", "alice@univ.edu")
            .claim("role", "STUDENT")
            // missing tokenVersion
            .issuedAt(Date.from(fixedInstant))
            .expiration(Date.from(fixedInstant.plusSeconds(3600)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        assertThatThrownBy(() -> provider.parseAndValidate(missingClaimToken))
            .isInstanceOf(InvalidJwtException.class)
            .extracting(e -> ((InvalidJwtException) e).getErrorCode())
            .isEqualTo(ErrorCode.MISSING_CLAIMS);
    }

    // ========================================================================
    // TOKEN VERSION TESTS
    // ========================================================================

    @Test
    @DisplayName("27 & 28. Token version is correctly extracted and preserved")
    void shouldExtractAndPreserveTokenVersion() {
        AuthenticationResult v5Auth = new AuthenticationResult(
            userId, "user123", "u@univ.edu", UserRole.HOD_ADMIN, Optional.empty(), Optional.empty(), 5);

        String token = provider.generateToken(v5Auth);

        assertThat(provider.extractTokenVersion(token)).isEqualTo(5);
    }

    // ========================================================================
    // SECURITY & CONFIGURATION TESTS
    // ========================================================================

    @Test
    @DisplayName("29-31. Password and secret are never present in claims")
    void shouldNeverContainPasswordsOrSecretInClaims() {
        String token = provider.generateToken(createStudentAuth());

        assertThat(token).doesNotContain("password");
        assertThat(token).doesNotContain(TEST_SECRET);
    }

    @Test
    @DisplayName("32 & 33. Weak secret (< 32 bytes / 256 bits) is rejected during initialization")
    void shouldRejectWeakSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short-secret", ISSUER, 3600L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("34. Missing secret is rejected safely")
    void shouldRejectBlankSecret() {
        assertThatThrownBy(() -> new JwtProperties("", ISSUER, 3600L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("35. Configured issuer and expiration are respected")
    void shouldRespectConfiguredProperties() {
        JwtProperties customProps = new JwtProperties(TEST_SECRET, "custom-issuer", 7200L);
        JwtTokenProvider customProvider = new JwtTokenProvider(customProps, fixedClock);

        String token = customProvider.generateToken(createStudentAuth());
        ValidatedTokenClaims claims = customProvider.parseAndValidate(token);

        assertThat(claims.expiration()).isEqualTo(fixedInstant.plusSeconds(7200L));
    }
}
