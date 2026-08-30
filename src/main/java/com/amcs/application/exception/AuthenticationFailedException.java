package com.amcs.application.exception;

public class AuthenticationFailedException extends RuntimeException {

    public enum FailureReason {
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        ACCOUNT_SUSPENDED,
        ACCOUNT_DEACTIVATED
    }

    private final FailureReason reason;

    public AuthenticationFailedException(String message, FailureReason reason) {
        super(message);
        this.reason = reason;
    }

    public FailureReason getReason() {
        return reason;
    }
}
