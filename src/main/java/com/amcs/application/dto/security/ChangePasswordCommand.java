package com.amcs.application.dto.security;

import java.util.Objects;
import java.util.UUID;

/**
 * Command for changing user password.
 */
public record ChangePasswordCommand(
    UUID userId,
    String currentPassword,
    String newPassword
) {
    public ChangePasswordCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(currentPassword, "currentPassword must not be null");
        Objects.requireNonNull(newPassword, "newPassword must not be null");
    }

    @Override
    public String toString() {
        return "ChangePasswordCommand[userId=" + userId + ", currentPassword=***, newPassword=***]";
    }
}
