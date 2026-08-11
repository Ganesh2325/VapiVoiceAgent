package com.voiceos.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler.
 * Catches all exceptions from REST controllers and converts them to
 * consistent {@link ErrorResponse} JSON responses.
 *
 * <p>Security principle: never leak internal error details (stack traces,
 * SQL queries, class names) to API clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Handles application-level exceptions with structured error codes. */
    @ExceptionHandler(VoiceOsException.class)
    public ResponseEntity<ErrorResponse> handleVoiceOsException(
            VoiceOsException ex, WebRequest request) {
        log.warn("VoiceOsException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(
                        ex.getHttpStatus().value(),
                        ex.getHttpStatus().getReasonPhrase(),
                        ex.getErrorCode(),
                        ex.getMessage(),
                        extractPath(request)
                ));
    }

    /** Handles @Valid / @Validated constraint violations. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage(),
                                fe.getRejectedValue());
                    }
                    return new ErrorResponse.FieldError(error.getObjectName(),
                            error.getDefaultMessage(), null);
                })
                .collect(Collectors.toList());

        log.debug("Validation failed: {} field error(s)", fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withFieldErrors(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "VALIDATION_ERROR",
                        "Request validation failed. Check fieldErrors for details.",
                        extractPath(request),
                        fieldErrors
                ));
    }

    /** Handles authentication failures (wrong password, etc.). */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "INVALID_CREDENTIALS",
                        "Invalid email or password.",
                        extractPath(request)
                ));
    }

    /** Handles authorization failures (insufficient permissions). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied for request to {}", extractPath(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "FORBIDDEN",
                        "You do not have permission to perform this action.",
                        extractPath(request)
                ));
    }

    /** Handles path variable / parameter type mismatches (e.g. non-UUID where UUID expected). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("Parameter '%s' has invalid value: %s",
                ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "INVALID_PARAMETER",
                        message,
                        extractPath(request)
                ));
    }

    /** Catch-all for unexpected exceptions. Logs full details internally. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error processing request to {}: {}",
                extractPath(request), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        extractPath(request)
                ));
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
