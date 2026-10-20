package com.hims.adt.application.exceptions;

/*
 * Base for ADT rule violations carrying a legacy error code (codes BERA022, BERB005,
 * BERB013, BEXB001, etc.).
 */
public abstract class AdtRuleException extends RuntimeException {

    private final String errorCode;

    protected AdtRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
