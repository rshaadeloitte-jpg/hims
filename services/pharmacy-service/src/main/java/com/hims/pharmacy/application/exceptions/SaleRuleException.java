package com.hims.pharmacy.application.exceptions;

/** Base rule violation carrying the legacy-style error code (contract per 04 §9.6). */
public class SaleRuleException extends RuntimeException {

    private final String errorCode;

    public SaleRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
