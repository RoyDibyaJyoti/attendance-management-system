package com.amcs.application.exception;

public class StudentNotEligibleException extends RuntimeException {
    public StudentNotEligibleException(String message) {
        super(message);
    }
}
