package com.amcs.application.exception;

public class InvalidPasswordException extends InvalidBusinessOperationException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
