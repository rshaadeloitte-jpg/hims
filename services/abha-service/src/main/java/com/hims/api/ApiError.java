package com.hims.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
    int status,
    String message,
    LocalDateTime timestamp,
    Map<String, String> errors
) {
    public ApiError(int status, String message) {
        this(status, message, LocalDateTime.now(), null);
    }
}