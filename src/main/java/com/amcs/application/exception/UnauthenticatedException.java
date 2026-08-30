package com.amcs.application.exception;

/**
 * Thrown when an application use case requires an authenticated actor,
 * but the current security context is unauthenticated.
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
