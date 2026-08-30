package com.amcs.application.exception;

/**
 * Thrown when an authenticated actor attempts an operation without required permissions
 * (e.g. IDOR violation, unassigned teaching scope, or unauthorized conductor modification).
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
