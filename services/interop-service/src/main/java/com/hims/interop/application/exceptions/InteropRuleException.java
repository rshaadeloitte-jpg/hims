package com.hims.interop.application.exceptions;

/**
 * Base rule violation carrying the legacy-style error code (contract per 04 §9.6).
 * Codes used by interop: BEXA003 (generic validation), INTEROP_UNKNOWN_EVENT_TYPE,
 * INTEROP_INVALID_MESSAGE_TYPE, INTEROP_ILLEGAL_TRANSITION (WhatsApp machine),
 * INTEROP_DUPLICATE_CORRELATION_ID (409), NOT_FOUND (unknown id/correlationId).
 */
public class InteropRuleException extends RuntimeException {

    private final String errorCode;

    public InteropRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
