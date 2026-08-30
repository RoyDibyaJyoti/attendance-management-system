package com.amcs.application.exception;

public class OptimisticLockingConflictException extends RuntimeException {
    public OptimisticLockingConflictException(String message) {
        super(message);
    }

    public OptimisticLockingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
