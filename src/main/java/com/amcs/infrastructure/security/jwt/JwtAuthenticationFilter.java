package com.amcs.infrastructure.security.jwt;

import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.infrastructure.security.principal.SecurityUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Perimeter authentication filter that extracts, cryptographically validates,
 * and sets the SecurityContext for incoming Bearer JWT tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserAccountRepositoryPort userAccountRepositoryPort;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
        JwtTokenProvider tokenProvider,
        UserAccountRepositoryPort userAccountRepositoryPort,
        AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenProvider = tokenProvider;
        this.userAccountRepositoryPort = userAccountRepositoryPort;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = authHeader.substring(7).trim();
        if (rawToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ValidatedTokenClaims claims;
        try {
            claims = tokenProvider.parseAndValidate(rawToken);
        } catch (InvalidJwtException ex) {
            String code = switch (ex.getErrorCode()) {
                case EXPIRED -> "TOKEN_EXPIRED";
                case INVALID_SIGNATURE -> "INVALID_TOKEN";
                case MALFORMED, INVALID_FORMAT -> "MALFORMED_TOKEN";
                case INVALID_ISSUER -> "INVALID_ISSUER";
                case UNSUPPORTED_ALGORITHM -> "UNSUPPORTED_ALGORITHM";
                default -> "INVALID_TOKEN";
            };
            String message = switch (ex.getErrorCode()) {
                case EXPIRED -> "JWT token has expired";
                case INVALID_SIGNATURE -> "JWT token signature is invalid or tampered";
                case MALFORMED, INVALID_FORMAT -> "JWT token structure is malformed";
                case INVALID_ISSUER -> "Invalid token issuer";
                case UNSUPPORTED_ALGORITHM -> "JWT token algorithm is unsupported";
                case MISSING_CLAIMS -> "JWT token is missing required claims";
                default -> "Invalid token";
            };
            request.setAttribute("AMCS_SECURITY_ERROR_CODE", code);
            request.setAttribute("AMCS_SECURITY_ERROR_MESSAGE", message);
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(message, ex)
            );
            return;
        }

        // Token-version and Account Status Verification
        Optional<UserAccount> accountOpt = userAccountRepositoryPort.findById(claims.userId());
        if (accountOpt.isEmpty()) {
            request.setAttribute("AMCS_SECURITY_ERROR_CODE", "INVALID_TOKEN");
            request.setAttribute("AMCS_SECURITY_ERROR_MESSAGE", "Account associated with token no longer exists");
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("User account not found")
            );
            return;
        }

        UserAccount account = accountOpt.get();
        if (account.status() != UserAccountStatus.ACTIVE) {
            request.setAttribute("AMCS_SECURITY_ERROR_CODE", "ACCOUNT_" + account.status().name());
            request.setAttribute("AMCS_SECURITY_ERROR_MESSAGE", "User account is " + account.status().name().toLowerCase());
            authenticationEntryPoint.commence(
                request,
                response,
                new DisabledException("Account is not active")
            );
            return;
        }

        if (account.tokenVersion() != claims.tokenVersion()) {
            request.setAttribute("AMCS_SECURITY_ERROR_CODE", "TOKEN_EXPIRED");
            request.setAttribute("AMCS_SECURITY_ERROR_MESSAGE", "Token has been revoked or invalidated by password change");
            authenticationEntryPoint.commence(
                request,
                response,
                new CredentialsExpiredException("Token version mismatch")
            );
            return;
        }

        // Set authenticated principal in SecurityContext
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            claims.userId(),
            claims.username(),
            claims.email(),
            claims.role(),
            claims.studentId(),
            claims.facultyId(),
            claims.tokenVersion()
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
