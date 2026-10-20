package com.hims.billing.application.exceptions;

/** Invalid payload / illegal lifecycle transition -> 400 with the rule code. */
public class InvalidBillingRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidBillingRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
