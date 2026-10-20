package com.hims.ordering.application.exceptions;

/** Base rule violation carrying the legacy-style error code (contract per 04 §9.6). */
public class OrderRuleException extends RuntimeException {

    private final String errorCode;

    public OrderRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
