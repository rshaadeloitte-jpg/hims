package com.hims.rcm.application.exceptions;

/** Base rule violation carrying the legacy-style error code (contract per 04 §9.6). */
public class RcmRuleException extends RuntimeException {

    private final String errorCode;

    public RcmRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
