package com.amcs.infrastructure.web.error;

import com.amcs.application.exception.DuplicateResourceException;
import com.amcs.application.exception.InvalidBusinessOperationException;
import com.amcs.application.exception.InvalidSessionTransitionException;
import com.amcs.application.exception.OptimisticLockingConflictException;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.exception.SessionStateConflictException;
import com.amcs.application.exception.StudentNotEligibleException;
import com.amcs.domain.attendance.AttendanceIntegrityException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
        HttpMessageNotReadableException ex, HttpServletRequest request
    ) {
        log.warn("Malformed JSON request at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(400, "MALFORMED_JSON", "Malformed or unreadable JSON payload", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
        org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request
    ) {
        log.warn("Missing request parameter [{}] at [{}]: {}", ex.getParameterName(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(400, "MISSING_PARAMETER", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        List<ValidationErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new ValidationErrorDetail(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
            ));
        }

        log.warn("Validation failed at [{}] with {} errors", request.getRequestURI(), details.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.withDetails(
                400,
                "VALIDATION_FAILED",
                "Request validation failed with " + details.size() + " error(s)",
                request.getRequestURI(),
                details
            ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, HttpServletRequest request
    ) {
        log.info("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(404, "RESOURCE_NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
        DuplicateResourceException ex, HttpServletRequest request
    ) {
        log.warn("Duplicate resource collision at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(409, "DUPLICATE_RESOURCE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SessionStateConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionStateConflict(
        SessionStateConflictException ex, HttpServletRequest request
    ) {
        log.warn("Session state conflict at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(409, "SESSION_ALREADY_CONDUCTED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockingConflictException.class})
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockConflict(
        Exception ex, HttpServletRequest request
    ) {
        log.warn("Optimistic lock conflict at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(
                409,
                "OPTIMISTIC_LOCK_CONFLICT",
                "The resource was modified concurrently by another transaction. Please reload and retry.",
                request.getRequestURI()
            ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseConflict(
        DataIntegrityViolationException ex, HttpServletRequest request
    ) {
        log.warn("Database integrity violation at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(
                409,
                "DATABASE_CONFLICT",
                "Operation conflicted with existing database records or unique constraints.",
                request.getRequestURI()
            ));
    }

    @ExceptionHandler(StudentNotEligibleException.class)
    public ResponseEntity<ApiErrorResponse> handleStudentNotEligible(
        StudentNotEligibleException ex, HttpServletRequest request
    ) {
        log.warn("Student not eligible at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorResponse.of(422, "STUDENT_NOT_ELIGIBLE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AttendanceIntegrityException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityViolation(
        AttendanceIntegrityException ex, HttpServletRequest request
    ) {
        log.warn("Attendance integrity violation at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorResponse.of(422, "ATTENDANCE_INTEGRITY_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidSessionTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSessionTransition(
        InvalidSessionTransitionException ex, HttpServletRequest request
    ) {
        log.warn("Invalid session state transition at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorResponse.of(422, "INVALID_SESSION_TRANSITION", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.amcs.application.exception.InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPassword(
        com.amcs.application.exception.InvalidPasswordException ex, HttpServletRequest request
    ) {
        log.warn("Invalid password at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(400, "INVALID_PASSWORD", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.amcs.application.exception.PasswordValidationException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordValidation(
        com.amcs.application.exception.PasswordValidationException ex, HttpServletRequest request
    ) {
        log.warn("Password policy violation at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of(400, "PASSWORD_POLICY_VIOLATION", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidBusinessOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBusinessOperation(
        InvalidBusinessOperationException ex, HttpServletRequest request
    ) {
        log.warn("Invalid business operation at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorResponse.of(422, "INVALID_BUSINESS_OPERATION", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.amcs.application.exception.AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(
        com.amcs.application.exception.AuthenticationFailedException ex, HttpServletRequest request
    ) {
        log.warn("Authentication failed at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of(401, ex.getReason().name(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.amcs.application.exception.UnauthenticatedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthenticated(
        com.amcs.application.exception.UnauthenticatedException ex, HttpServletRequest request
    ) {
        log.warn("Unauthenticated request at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse.of(401, "UNAUTHORIZED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
        org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request
    ) {
        log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of(403, "FORBIDDEN", "Access is denied: insufficient privileges", request.getRequestURI()));
    }

    @ExceptionHandler(com.amcs.application.exception.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationAccessDenied(
        com.amcs.application.exception.AccessDeniedException ex, HttpServletRequest request
    ) {
        log.warn("Application access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of(403, "ACCESS_DENIED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleCatchAll(
        Exception ex, HttpServletRequest request
    ) {
        log.error("Unhandled internal error at [{}]:", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(
                500,
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please contact the administrator.",
                request.getRequestURI()
            ));
    }
}
