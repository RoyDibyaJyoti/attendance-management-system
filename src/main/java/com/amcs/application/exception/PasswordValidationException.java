package com.amcs.application.exception;

public class PasswordValidationException extends InvalidBusinessOperationException {

    public PasswordValidationException(String message) {
        super(message);
    }
}
