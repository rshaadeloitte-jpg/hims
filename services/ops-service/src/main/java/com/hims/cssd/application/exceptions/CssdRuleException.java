package com.hims.cssd.application.exceptions;

/**
 * Base rule violation carrying the legacy-style error code (contract per 04 §9.6).
 * Codes used by CSSD: BEXA003 (generic validation), CSSD_DUPLICATE_KIT_CODE,
 * CSSD_ILLEGAL_TRANSITION (lifecycle guards), CSSD_CYCLE_ALREADY_COMPLETED,
 * NOT_FOUND (unknown kit/cycle).
 */
public class CssdRuleException extends RuntimeException {

    private final String errorCode;

    public CssdRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
