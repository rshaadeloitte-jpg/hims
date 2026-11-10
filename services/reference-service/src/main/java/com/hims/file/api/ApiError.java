package com.hims.file.api;

/** New-contract error envelope (same shape as reference/item/grievance services). */
public record ApiError(String status, String errorCode, String message) {

    public static ApiError of(String status, String errorCode, String message) {
        return new ApiError(status, errorCode, message);
    }
}
