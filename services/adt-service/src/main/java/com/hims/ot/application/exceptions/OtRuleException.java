package com.hims.ot.application.exceptions;

/** Base for business-rule violations carrying a legacy error code. */
public class OtRuleException extends RuntimeException {

    private final String errorCode;

    public OtRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
