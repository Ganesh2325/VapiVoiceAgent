package com.voiceos.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all VoiceOS application errors.
 * Carries an HTTP status code for proper API error responses.
 */
public class VoiceOsException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public VoiceOsException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public VoiceOsException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ─── Convenience Factory Methods ─────────────────────────────────────────

    public static VoiceOsException notFound(String resource, Object id) {
        return new VoiceOsException(
                resource + " not found with id: " + id,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND"
        );
    }

    public static VoiceOsException badRequest(String message) {
        return new VoiceOsException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static VoiceOsException forbidden(String message) {
        return new VoiceOsException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static VoiceOsException conflict(String message) {
        return new VoiceOsException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static VoiceOsException serviceUnavailable(String message) {
        return new VoiceOsException(message, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }
}
