package com.amcs.application.service;

import com.amcs.application.exception.PasswordValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates passwords against institutional security policy requirements.
 */
@Component
public class PasswordValidator {

    private final int minLength;
    private final int maxLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordValidator(
        @Value("${amcs.security.password.min-length:8}") int minLength,
        @Value("${amcs.security.password.max-length:128}") int maxLength,
        @Value("${amcs.security.password.require-uppercase:true}") boolean requireUppercase,
        @Value("${amcs.security.password.require-lowercase:true}") boolean requireLowercase,
        @Value("${amcs.security.password.require-digit:true}") boolean requireDigit,
        @Value("${amcs.security.password.require-special:true}") boolean requireSpecial
    ) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecial = requireSpecial;
    }

    public void validate(String password) {
        if (password == null || password.length() < minLength) {
            throw new PasswordValidationException("Password must be at least " + minLength + " characters long");
        }
        if (password.length() > maxLength) {
            throw new PasswordValidationException("Password must not exceed " + maxLength + " characters");
        }
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            throw new PasswordValidationException("Password must contain at least one uppercase letter");
        }
        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            throw new PasswordValidationException("Password must contain at least one lowercase letter");
        }
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            throw new PasswordValidationException("Password must contain at least one digit");
        }
        if (requireSpecial && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new PasswordValidationException("Password must contain at least one special character or symbol");
        }
    }
}
