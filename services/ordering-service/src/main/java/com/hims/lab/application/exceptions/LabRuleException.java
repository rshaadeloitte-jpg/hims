package com.hims.lab.application.exceptions;

/*
 * Base for Lab rule violations carrying an error code (LAB_REQ_INVALID,
 * LAB_ILLEGAL_TRANSITION, LAB_NOT_FOUND, ...).
 */
public abstract class LabRuleException extends RuntimeException {

    private final String errorCode;

    protected LabRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
