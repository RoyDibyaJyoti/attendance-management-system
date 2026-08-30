package com.amcs.application.dto.security;

import java.util.Objects;

/**
 * Command for authenticating a user account.
 */
public record LoginCommand(
    String usernameOrEmail,
    String password
) {
    public LoginCommand {
        Objects.requireNonNull(usernameOrEmail, "usernameOrEmail must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }

    @Override
    public String toString() {
        return "LoginCommand[usernameOrEmail=" + usernameOrEmail + ", password=***]";
    }
}
