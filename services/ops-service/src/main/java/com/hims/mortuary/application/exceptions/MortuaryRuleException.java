package com.hims.mortuary.application.exceptions;

/**
 * Base for mortuary rule violations carrying an error code.
 */
public abstract class MortuaryRuleException extends RuntimeException {

    private final String errorCode;

    protected MortuaryRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
