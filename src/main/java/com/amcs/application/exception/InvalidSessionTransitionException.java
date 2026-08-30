package com.amcs.application.exception;

public class InvalidSessionTransitionException extends RuntimeException {
    public InvalidSessionTransitionException(String message) {
        super(message);
    }
}
