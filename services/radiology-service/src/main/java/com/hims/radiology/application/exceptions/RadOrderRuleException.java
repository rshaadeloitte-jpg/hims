package com.hims.radiology.application.exceptions;

/** Base rule violation carrying the legacy-style error code (contract per 04 §9.6). */
public class RadOrderRuleException extends RuntimeException {

    private final String errorCode;

    public RadOrderRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
