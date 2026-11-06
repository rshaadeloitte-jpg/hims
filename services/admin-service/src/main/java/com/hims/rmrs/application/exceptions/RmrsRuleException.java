package com.hims.rmrs.application.exceptions;

/** Base for RMRS business-rule violations carrying a BEXA-style error code (sibling error envelope). */
public abstract class RmrsRuleException extends RuntimeException {

    private final String errorCode;

    protected RmrsRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
