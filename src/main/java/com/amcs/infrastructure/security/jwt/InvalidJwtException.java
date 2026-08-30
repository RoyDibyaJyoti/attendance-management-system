package com.amcs.infrastructure.security.jwt;

/**
 * Exception thrown when a JWT token fails cryptographic or structural validation.
 */
public class InvalidJwtException extends RuntimeException {

    public enum ErrorCode {
        EXPIRED,
        INVALID_SIGNATURE,
        MALFORMED,
        INVALID_ISSUER,
        MISSING_CLAIMS,
        UNSUPPORTED_ALGORITHM,
        INVALID_FORMAT
    }

    private final ErrorCode errorCode;

    public InvalidJwtException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidJwtException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
