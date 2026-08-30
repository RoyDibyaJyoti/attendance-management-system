package com.amcs.infrastructure.security.error;

import com.amcs.infrastructure.web.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Authentication entry point rendering standard ApiErrorResponse on 401 Unauthorized.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null ? objectMapper.copy() : new ObjectMapper())
            .findAndRegisterModules();
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        String code = (String) request.getAttribute("AMCS_SECURITY_ERROR_CODE");
        if (code == null) {
            code = "UNAUTHORIZED";
        }
        String message = (String) request.getAttribute("AMCS_SECURITY_ERROR_MESSAGE");
        if (message == null) {
            message = "Full authentication is required to access this resource";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.of(
            HttpServletResponse.SC_UNAUTHORIZED,
            code,
            message,
            request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
