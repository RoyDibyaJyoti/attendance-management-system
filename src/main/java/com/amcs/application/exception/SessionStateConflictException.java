package com.amcs.application.exception;

public class SessionStateConflictException extends RuntimeException {
    public SessionStateConflictException(String message) {
        super(message);
    }
}
