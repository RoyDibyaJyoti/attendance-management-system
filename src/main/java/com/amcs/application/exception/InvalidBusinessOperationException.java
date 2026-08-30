package com.amcs.application.exception;

public class InvalidBusinessOperationException extends RuntimeException {
    public InvalidBusinessOperationException(String message) {
        super(message);
    }
}
