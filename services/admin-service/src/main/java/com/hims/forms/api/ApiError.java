package com.hims.forms.api;

public record ApiError(String status, String errorCode, String message) {
    public static ApiError of(String status, String errorCode, String message) {
        return new ApiError(status, errorCode, message);
    }
}
