package com.voiceos.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Structured error response body returned for all API errors.
 * Consistent format ensures frontend can parse errors reliably.
 */
public record ErrorResponse(
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    /** Constructor for simple errors without field-level detail. */
    public static ErrorResponse of(int status, String error, String errorCode,
                                   String message, String path) {
        return new ErrorResponse(status, error, errorCode, message, path,
                Instant.now(), List.of());
    }

    /** Constructor for validation errors with field-level detail. */
    public static ErrorResponse withFieldErrors(int status, String error, String errorCode,
                                                String message, String path,
                                                List<FieldError> fieldErrors) {
        return new ErrorResponse(status, error, errorCode, message, path,
                Instant.now(), fieldErrors);
    }

    /** Per-field validation error detail. */
    public record FieldError(String field, String message, Object rejectedValue) {}
}
