package com.insurance.dashboard.api.exception;

import java.time.LocalDateTime;

/** HTTP error response body. */
public record ErrorResponse(LocalDateTime timestamp, int status, String message) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, message);
    }
}
