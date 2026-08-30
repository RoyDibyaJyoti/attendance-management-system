package com.amcs.infrastructure.excel.security;

/**
 * Exception thrown when an uploaded spreadsheet fails structural, security, or OOXML verification.
 */
public class InvalidSpreadsheetException extends RuntimeException {

    private final String errorCode;

    public InvalidSpreadsheetException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidSpreadsheetException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
