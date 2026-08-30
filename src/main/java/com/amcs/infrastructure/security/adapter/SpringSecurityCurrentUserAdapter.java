package com.amcs.infrastructure.security.adapter;

import com.amcs.application.exception.UnauthenticatedException;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.CurrentUserPort;
import com.amcs.infrastructure.security.principal.SecurityUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Infrastructure adapter translating Spring Security's SecurityContext into the application-level AuthenticatedActor.
 */
@Component
public class SpringSecurityCurrentUserAdapter implements CurrentUserPort {

    @Override
    public Optional<AuthenticatedActor> getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof SecurityUserPrincipal principal)) {
            return Optional.empty();
        }

        return Optional.of(new AuthenticatedActor(
            principal.userId(),
            principal.getUsername(),
            principal.role(),
            principal.studentId(),
            principal.facultyId()
        ));
    }

    @Override
    public AuthenticatedActor requireCurrentActor() {
        return getCurrentActor().orElseThrow(() ->
            new UnauthenticatedException("No authenticated actor found in current security context")
        );
    }
}
