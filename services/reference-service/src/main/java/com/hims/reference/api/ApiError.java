package com.hims.reference.api;

/**
 * New-contract error envelope. Legacy error handling (ErrorDetail + icu bundle)
 * parity for the message catalog (BEXA003 etc.) is a MIG-002 fixture item; the
 * envelope shape below is the S0 baseline the gateway can map.
 */
public record ApiError(String status, String errorCode, String message) {

    public static ApiError of(String status, String errorCode, String message) {
        return new ApiError(status, errorCode, message);
    }
}
