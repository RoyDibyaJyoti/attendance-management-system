package com.amcs.application.port.out.security;

import java.util.Optional;

/**
 * Outbound application port for resolving the currently authenticated actor.
 * Shields the application service layer from Spring Security's SecurityContextHolder.
 */
public interface CurrentUserPort {

    /**
     * Returns the currently authenticated actor, or empty if unauthenticated.
     */
    Optional<AuthenticatedActor> getCurrentActor();

    /**
     * Returns the currently authenticated actor, throwing UnauthenticatedException if missing.
     */
    AuthenticatedActor requireCurrentActor();
}
