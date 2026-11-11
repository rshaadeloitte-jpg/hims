package com.hims.payments.application.exceptions;

/** Invalid payload / rule violation -> 400 with the rule code (same shape as sibling services). */
public class InvalidPaymentRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidPaymentRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
